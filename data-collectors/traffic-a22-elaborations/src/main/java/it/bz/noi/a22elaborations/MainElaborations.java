// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22elaborations;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;

@Component
public class MainElaborations {

	private static final int NULL_VALUE = -999;

	static class WindowStepLength {
		int step;
		int length;
	}

	private static final Logger LOG = LoggerFactory.getLogger(MainElaborations.class);

	@Autowired
	private A22TrafficJSONPusher pusher;

	@Autowired
	private SyncStation syncStation;

	@Autowired
	private SyncDatatype syncDataType;

	@Autowired
	private Utility utility;
	
	@Autowired
	private EUROTypeUtil euroUtility;

	@PostConstruct
	private void init() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void execute() {
		try {
			LOG.debug("Start MainElaborations");

			LOG.debug("Sync datatypes");
			syncDataType.saveDatatypes();

			LOG.debug("Create connection");
			Connection connection = utility.getConnection();

			LOG.debug("Sync stations");
			StationList stations = syncStation.saveStations(connection);
			LOG.debug("Length stationList: " + stations.size());

			LOG.debug("Read elaboration properties");
			WindowStepLength winStepLength = readElaborationProperties();
		
			long now = System.currentTimeMillis();

			for (StationDto station : stations) {

				String stationcode = station.getId();
				LOG.debug("Stationcode: " + stationcode);
				// System.out.println(stationcode);
				// System.out.println(station.getName());

				long lastTimestamp = ((java.util.Date) pusher.getDateOfLastRecord(stationcode, null, null)).getTime();
				// LOG.debug("Timestamp of last record: " + lastTimestamp);
				if (lastTimestamp <= 0) {
					Calendar calendar = Calendar.getInstance();
					// minimum import date
					calendar.set(2024, Calendar.JULY, 10, 0, 0, 0);
					calendar.set(Calendar.MILLISECOND, 0);

					lastTimestamp = calendar.getTimeInMillis();
				} else {
					// // 2019-07-10 d@vide.bz: go back 1 hour from last time elaboration because
					// data
					// // can't be in realtime
					lastTimestamp -= 3600L * 1000L;
				}

				// 2022-07-05 d@vide.bz: temporary workaround that reduce the problem of
				// pushData not updating values
				// don't write data for windows near now because the probability that are
				// incomplete is high
				long horizon = now - 45L * 60L * 1000L;
				LOG.debug("Horizon: " + horizon);

				// loop over the windows
				while (lastTimestamp + winStepLength.length < horizon) // loop until the window is entirely in the past
				{
					LOG.debug("elaborating station: " + station.getName() + " window from: "
							+ new java.sql.Timestamp(lastTimestamp).toString()
							+ " to: " + new java.sql.Timestamp(lastTimestamp + winStepLength.length).toString());
					List<Vehicle> vehicles = readWindow(lastTimestamp, lastTimestamp + winStepLength.length,
							stationcode, connection);

					LOG.debug("Vehicles in window " + vehicles.size());
					LOG.debug("Save measurement and calculations");
					saveMeasurementAndCalculation(station, vehicles, lastTimestamp, winStepLength.length);

					lastTimestamp = lastTimestamp + winStepLength.step;
				}
			}
			connection.close();
		} catch (Exception exxx) {
			throw new IllegalStateException(exxx);
		}
		LOG.debug("Finish writing.");
	}

	private WindowStepLength readElaborationProperties() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("elaborations.properties")) {
			LOG.debug("Elaboration properties: ");
			Properties prop = new Properties();
			prop.load(in);
			WindowStepLength result = new WindowStepLength();
			result.length = Integer.parseInt(prop.getProperty("windowLength"));
			LOG.debug("Window length: " + result.length);
			result.step = Integer.parseInt(prop.getProperty("step"));
			LOG.debug("Step: " + result.step);
			return result;
		}
	}

	/**
	 * saves all calculations in bdp-core
	 *
	 */
	private void saveMeasurementAndCalculation(StationDto station, List<Vehicle> vehicles, long lastTimestamp,
			long windowLength) {
		DataMapDto<RecordDtoImpl> dataMapDto = new DataMapDto<>();

		if (SensorTypeUtil.isCamera(station)) {
			LOG.debug("Create EURO emission distribution");
			Timestamp timestamp = new Timestamp(lastTimestamp /* + windowLength / 2 */);
			Map<String, Double> euroPcts = createVehicleEuro(vehicles, timestamp);	
			SimpleRecordDto rec = new SimpleRecordDto(timestamp.getTime(), euroPcts,
					(int) (windowLength / 1000), System.currentTimeMillis());

			LOG.debug("Save Euro emission standard");
			dataMapDto.addRecord(station.getId(), SyncDatatype.EURO_CATEGOY_PCT, rec);
		}	
		
		if (SensorTypeUtil.isCamera(station)) {
			LOG.debug("Create Nationality aggregation");
			Map<String, Integer> countByNat = createVehicleNationality(vehicles);	
			Timestamp timestamp = new Timestamp(lastTimestamp /* + windowLength / 2 */);
			SimpleRecordDto rec = new SimpleRecordDto(timestamp.getTime(), countByNat,
					(int) (windowLength / 1000), System.currentTimeMillis());

			LOG.debug("Save Vehicle count by nationality" + countByNat.size());
			dataMapDto.addRecord(station.getId(), SyncDatatype.PLATE_NATIONALITY_COUNT, rec);
		}	
		
		LOG.debug("Create vehicle classes");
		Map<String, Integer> classCounts = createVehicleCounts(vehicles);

		double equivalentVehicles;
		double nrLightVehicles = 0;
		double nrHeavyVehicles = 0;
		double nrBuses = 0;

		for (Map.Entry<String, Integer> classCount : classCounts.entrySet()) {

			if (classCount.getKey().equals(SyncDatatype.NR_LIGHT_VEHICLES)) {
				nrLightVehicles += classCount.getValue();
			} else if (classCount.getKey().equals(SyncDatatype.NR_HEAVY_VEHICLES)) {
				nrHeavyVehicles += classCount.getValue();
			} else if (classCount.getKey().equals(SyncDatatype.NR_BUSES)) {
				nrBuses += classCount.getValue();
			}

			Timestamp timestamp = new Timestamp(lastTimestamp /* + windowLength / 2 */);
			SimpleRecordDto rec = new SimpleRecordDto(timestamp.getTime(), classCount.getValue(),
					(int) (windowLength / 1000), System.currentTimeMillis());

			LOG.debug("Save " + classCount.getKey() + " vehicles: " + classCount.getValue());
			dataMapDto.addRecord(station.getId(), classCount.getKey(), rec);

		}

		equivalentVehicles = nrLightVehicles + 2.5 * (nrHeavyVehicles + nrBuses);
		Timestamp timestamp = new Timestamp(lastTimestamp /* + windowLength / 2 */);
		SimpleRecordDto rec = new SimpleRecordDto(timestamp.getTime(), equivalentVehicles,
				(int) (windowLength / 1000), System.currentTimeMillis());
		LOG.debug("Save " + SyncDatatype.NR_EQUIVALENT_VEHICLES + " vehicles: " + equivalentVehicles);
		dataMapDto.addRecord(station.getId(), SyncDatatype.NR_EQUIVALENT_VEHICLES, rec);

		Map<String, Double> classAvgSpeeds = createClassAvgSpeeds(vehicles);

		for (Map.Entry<String, Double> classAvgSpeed : classAvgSpeeds.entrySet()) {
			Timestamp timestampAvgSpeed = new Timestamp(lastTimestamp /* + windowLength / 2 */);
			SimpleRecordDto recordAvgSpeed = new SimpleRecordDto(timestampAvgSpeed.getTime(), classAvgSpeed.getValue(),
					(int) (windowLength / 1000), System.currentTimeMillis());
			LOG.debug("Save avg speed: " + classAvgSpeed.getValue());
			dataMapDto.addRecord(station.getId(), classAvgSpeed.getKey(), recordAvgSpeed);
		}

		Map<String, Double> classVarSpeeds = createClassVarSpeeds(vehicles, classAvgSpeeds);

		for (Map.Entry<String, Double> classVarSpeed : classVarSpeeds.entrySet()) {
			Timestamp timestampVarSpeed = new Timestamp(lastTimestamp /* + windowLength / 2 */);
			SimpleRecordDto recordVarSpeed = new SimpleRecordDto(timestampVarSpeed.getTime(), classVarSpeed.getValue(),
					(int) (windowLength / 1000), System.currentTimeMillis());
			LOG.debug("Save speed variance " + classVarSpeed.getKey() + ": " + classVarSpeed.getValue());
			dataMapDto.addRecord(station.getId(), classVarSpeed.getKey(), recordVarSpeed);
		}

		Map<String, Double> classAvgs = createClassAvgs(vehicles, equivalentVehicles, windowLength);

		for (Map.Entry<String, Double> classAvg : classAvgs.entrySet()) {
			Timestamp timestampAvg = new Timestamp(lastTimestamp /* + windowLength / 2 */);
			SimpleRecordDto recordAvg = new SimpleRecordDto(timestampAvg.getTime(), classAvg.getValue(),
					(int) (windowLength / 1000), System.currentTimeMillis());
			LOG.debug("Save " + classAvg.getKey() + ": " + classAvg.getValue());
			dataMapDto.addRecord(station.getId(), classAvg.getKey(), recordAvg);

		}

		pusher.pushData(dataMapDto);

	}

	/**
	 * create averages for different vehicle classes
	 *
	 * @param vehicles
	 * @param equivalentVehicles
	 * @return map of average for each vehicle class
	 */
	private Map<String, Double> createClassAvgs(List<Vehicle> vehicles, double equivalentVehicles,
			long windowLength) {
		Map<String, Double> classCounts = new HashMap<>();

		double gapSum = 0;
		double headwaySum = 0;
		// double meanSpacing = 0;
		double speedSum = 0;

		for (Vehicle vehicle : vehicles) {
			gapSum += vehicle.getDistance();
			headwaySum += vehicle.getHeadway();
			// meanSpacing += vehicle.getHeadway() * vehicle.getSpeed() / 3.6;
			speedSum += vehicle.getSpeed();
		}
		double avgHeadway = NULL_VALUE;
		double averageDensity = NULL_VALUE;
		// calculate avgFlow from vehicles in interval and convert sec to h
		double averageFlow = equivalentVehicles * 3600 * 1000 / windowLength;
		double averageGap = NULL_VALUE;

		if (!vehicles.isEmpty()) {
			avgHeadway = headwaySum / vehicles.size();
			// meanSpacing = meanSpacing / vehicles.size();
			// averageDensity = 1/(meanSpacing/1000);
			// averageFlow = (1/avgHeadway)*3600;

			double averageSpeed = speedSum / vehicles.size();
			averageDensity = averageSpeed == 0 ? 0 : (averageFlow / averageSpeed);
			averageGap = gapSum / vehicles.size();
		}

		classCounts.put(SyncDatatype.AVERAGE_GAP, averageGap);
		classCounts.put(SyncDatatype.AVERAGE_HEADWAY, avgHeadway);
		classCounts.put(SyncDatatype.AVERAGE_DENSITY, averageDensity);
		classCounts.put(SyncDatatype.AVERAGE_FLOW, averageFlow);
		return classCounts;
	}

	/**
	 * creates speed variance of each vehicle class
	 *
	 * @param vehicles
	 * @param classAvgSpeeds
	 * @return map of each vehicles class with the speed variance
	 */
	private static Map<String, Double> createClassVarSpeeds(List<Vehicle> vehicles,
			Map<String, Double> classAvgSpeeds) {
		Map<String, Double> classCounts = new HashMap<>();

		double speedSumLight = 0.0;
		double speedSumHeavy = 0.0;
		double speedSumBuses = 0.0;
		int countLight = 0;
		int countHeavy = 0;
		int countBuses = 0;
		for (Vehicle vehicle : vehicles) {
			if (vehicle.isLight()) {
				speedSumLight += Math
						.pow(vehicle.getSpeed() - classAvgSpeeds.get(SyncDatatype.AVERAGE_SPEED_LIGHT_VEHICLES), 2);
				countLight++;
			} else if (vehicle.isHeavy()) {
				speedSumHeavy += Math
						.pow(vehicle.getSpeed() - classAvgSpeeds.get(SyncDatatype.AVERAGE_SPEED_HEAVY_VEHICLES), 2);
				countHeavy++;
			} else if (vehicle.isBus()) {
				speedSumBuses += Math.pow(vehicle.getSpeed() - classAvgSpeeds.get(SyncDatatype.AVERAGE_SPEED_BUSES), 2);
				countBuses++;
			}
		}

		double speedVarLight = NULL_VALUE;
		double speedVarHeavy = NULL_VALUE;
		double speedVarBuses = NULL_VALUE;

		if (countLight > 0) {
			speedVarLight = speedSumLight / countLight;
		}
		if (countHeavy > 0) {
			speedVarHeavy = speedSumHeavy / countHeavy;
		}
		if (countBuses > 0) {
			speedVarBuses = speedSumBuses / countBuses;
		}

		classCounts.put(SyncDatatype.VARIANCE_SPEED_LIGHT_VEHICLES, speedVarLight);
		classCounts.put(SyncDatatype.VARIANCE_SPEED_HEAVY_VEHICLES, speedVarHeavy);
		classCounts.put(SyncDatatype.VARIANCE_SPEED_BUSES, speedVarBuses);

		return classCounts;
	}

	/**
	 * create for every vehicle class average speed
	 *
	 * @param vehicles
	 * @return map of vehicle class with average speed
	 */
	private static Map<String, Double> createClassAvgSpeeds(List<Vehicle> vehicles) {
		Map<String, Double> classCounts = new HashMap<>();

		double speedSumLight = 0.0;
		double speedSumHeavy = 0.0;
		double speedSumBuses = 0.0;
		int countLight = 0;
		int countHeavy = 0;
		int countBuses = 0;
		for (Vehicle vehicle : vehicles) {
			if (vehicle.isLight()) {
				speedSumLight += vehicle.getSpeed();
				countLight++;
			} else if (vehicle.isHeavy()) {
				speedSumHeavy += vehicle.getSpeed();
				countHeavy++;
			} else if (vehicle.isBus()) {
				speedSumBuses += vehicle.getSpeed();
				countBuses++;
			}
		}

		double speedAvgLight = NULL_VALUE;
		double speedAvgHeavy = NULL_VALUE;
		double speedAvgBuses = NULL_VALUE;

		if (countLight > 0) {
			speedAvgLight = speedSumLight / countLight;
		}
		if (countHeavy > 0) {
			speedAvgHeavy = speedSumHeavy / countHeavy;
		}
		if (countBuses > 0) {
			speedAvgBuses = speedSumBuses / countBuses;
		}

		classCounts.put(SyncDatatype.AVERAGE_SPEED_LIGHT_VEHICLES, speedAvgLight);
		classCounts.put(SyncDatatype.AVERAGE_SPEED_HEAVY_VEHICLES, speedAvgHeavy);
		classCounts.put(SyncDatatype.AVERAGE_SPEED_BUSES, speedAvgBuses);

		return classCounts;
	}

	/**
	 * read all vehicles in interval
	 *
	 * @param from_ts
	 * @param to_ts
	 * @return list of vehicle objects
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<Vehicle> readWindow(long from_ts, long to_ts, String stationcode, Connection connection)
			throws SQLException, IOException {
		ArrayList<Vehicle> vehicles = new ArrayList<>();

		String query = Utility.readResourceText(MainElaborations.class, "read-window.sql");

		try (
				PreparedStatement ps = connection.prepareStatement(query);) {
			ps.setLong(1, from_ts / 1000);
			ps.setLong(2, to_ts / 1000);
			ps.setString(3, stationcode);
			ResultSet resultSet = ps.executeQuery();
			while (resultSet.next()) {
				Vehicle vehicle = new Vehicle(resultSet.getString("stationCode"), resultSet.getLong("timestamp"),
						resultSet.getDouble("distance"), resultSet.getDouble("headway"), resultSet.getDouble("length"),
						resultSet.getInt("axles"), resultSet.getBoolean("against_traffic"), resultSet.getInt("class"),
						resultSet.getDouble("speed"), resultSet.getInt("direction"), resultSet.getString("license_plate_initials"),
						resultSet.getString("country"));

					vehicles.add(vehicle);
			}
		}

		return vehicles;
	}

	/**
	 * create vehicle counts for every vehicle class
	 *
	 * @param vehicles
	 * @return map of vehicles classes and count
	 */
	public static Map<String, Integer> createVehicleCounts(List<Vehicle> vehicles) {
		Map<String, Integer> classCounts = new HashMap<>();
		classCounts.put(SyncDatatype.NR_LIGHT_VEHICLES, 0);
		classCounts.put(SyncDatatype.NR_HEAVY_VEHICLES, 0);
		classCounts.put(SyncDatatype.NR_BUSES, 0);
		for (Vehicle vehicle : vehicles) {
			if (vehicle.isLight()) {
				classCounts.put(SyncDatatype.NR_LIGHT_VEHICLES, classCounts.get(SyncDatatype.NR_LIGHT_VEHICLES) + 1);
			} else if (vehicle.isHeavy()) {
				classCounts.put(SyncDatatype.NR_HEAVY_VEHICLES, classCounts.get(SyncDatatype.NR_HEAVY_VEHICLES) + 1);
			} else if (vehicle.isBus()) {
				classCounts.put(SyncDatatype.NR_BUSES, classCounts.get(SyncDatatype.NR_BUSES) + 1);
			}
		}

		return classCounts;
	}

	/**
	 * create vehicle EURO probability distribution
	 *
     * @param vehicles    List of Vehicle objects
	 * @return map of vehicles EURO and probability
	 */
	public Map<String, Double> createVehicleEuro(List<Vehicle> vehicles, Timestamp timestamp) {
		Map<String, Double> euroProb = new HashMap<>();
		euroProb.put(EUROTypeUtil.EURO0, 0.0);
		euroProb.put(EUROTypeUtil.EURO1, 0.0);
		euroProb.put(EUROTypeUtil.EURO2, 0.0);
		euroProb.put(EUROTypeUtil.EURO3, 0.0);
		euroProb.put(EUROTypeUtil.EURO4, 0.0);
		euroProb.put(EUROTypeUtil.EURO5, 0.0);
		euroProb.put(EUROTypeUtil.EURO6, 0.0);
		euroProb.put(EUROTypeUtil.EUROE, 0.0);
		int validVehicleCount = 0;

		Map<String, EUROType> euroTypeMap = euroUtility.getVehicleDataMap(timestamp);

        for (Vehicle vehicle : vehicles) {
            String plateInitials = vehicle.getPlateIntiails();

            // Handle null or missing plate initials
            if (plateInitials == null || plateInitials.isEmpty()) {
                continue;
            }

            // Get EURO probabilities from the CSV-based map
            EUROType euroData = euroTypeMap.get(plateInitials);

            // If there's no mapping for this plate, continue
            if (euroData == null) {
                continue;
            }

            // Extract probability map and add to cumulative sum
            Map<String, Double> probabilities = euroData.getProbabilities();
            for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
                euroProb.put(entry.getKey(), euroProb.get(entry.getKey()) + entry.getValue());
            }

            validVehicleCount++;
        }

        // Normalize by dividing each sum by the valid vehicle count
        if (validVehicleCount > 0) {
            for (String key : euroProb.keySet()) {
                euroProb.put(key, euroProb.get(key) / validVehicleCount);
            }
        }

        return euroProb;
	}

	/**
	 * create vehicle Nationality count
	 *
     * @param vehicles    List of Vehicle objects
	 * @return map of vehicles count by nationality
	 */
	public Map<String, Integer> createVehicleNationality(List<Vehicle> vehicles) {
		Map<String, Integer> natCount = new HashMap<>();
		natCount.put("I", 0);
		natCount.put("F", 0);
		natCount.put("GB", 0);
		natCount.put("D", 0);
		natCount.put("CH", 0);
		natCount.put("A", 0);
		natCount.put("NL", 0);
		natCount.put("E", 0);
		natCount.put("B", 0);
		natCount.put("DK", 0);
		natCount.put("L", 0);
		natCount.put("S", 0);
		natCount.put("PL", 0);
		natCount.put("GR", 0);
		natCount.put("H", 0);
		natCount.put("CZ", 0);
		natCount.put("SK", 0);
		natCount.put("BG", 0);
		natCount.put("EST", 0);
		natCount.put("FIN", 0);
		natCount.put("HR", 0);
		natCount.put("IRL", 0);
		natCount.put("LT", 0);
		natCount.put("LV", 0);
		natCount.put("P", 0);
		natCount.put("RO", 0);
		natCount.put("RSM", 0);
		natCount.put("SLO", 0);
		natCount.put("XXX", 0);
		
        for (Vehicle vehicle : vehicles) {
            String nation = vehicle.getPlateNat();

            // Handle null or missing plate initials
            if (nation == null || nation.isEmpty()) {
                continue;
            }

            // Get EURO probabilities from the CSV-based map
            if (null == natCount.get(nation)) {
				natCount.put(nation, 1);
				continue;
			}

            natCount.put(nation, natCount.get(nation) + 1);
		}

        return natCount;
	}

	/*
	 * Method used only for development/debugging
	 */
	public static void main(String[] args) {
		new MainElaborations().execute();
	}

}
