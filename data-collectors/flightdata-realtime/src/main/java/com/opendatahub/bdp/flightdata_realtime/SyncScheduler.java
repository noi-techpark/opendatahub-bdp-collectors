// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.opendatahub.bdp.flightdata_realtime;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import java.time.Instant;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;

@Service
public class SyncScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SyncScheduler.class);

	private static final String DATATYPE_ID = "scheduled-flight-adjustment";

	@Value("${odh_client.period}")
    private Integer period;

	@Value("${odh_client.endpoint}")
    private String endpoint;

	@Value("${odh_client.token}")
    private String token;

    @Lazy
    @Autowired
    private OdhClient odhClient;

    @Scheduled(cron = "${scheduler.job_a}")
    public void syncRealtimeFlights() {
        LOG.info("Sync Stations, Datatypes & Measurements...");

		DataMapDto<RecordDtoImpl> rootMap = new DataMapDto<>();

		DataFetcher fetcher = new DataFetcher();

		List<Map<String,String>> corrections = new ArrayList<>();

		try {
			List<Map<String,String>> realtime_arrivals = fetcher.getData(endpoint, token, "Departure");
			List<Map<String,String>> realtime_departures = fetcher.getData(endpoint, token, "Destination");

			corrections.addAll(realtime_arrivals);
			corrections.addAll(realtime_departures);
		} catch(Exception e) {
			LOG.error("Error fetching flight data: {}", e.getMessage());
		}

		StationList odhStationList = new StationList();

		for (int i = 0; i < corrections.size(); i++) {
			Map<String,String> externalStation = corrections.get(i);

			String odhStationCode = externalStation.get("FlightLogID") + "_" + externalStation.get("STD");

			// station data
			StationDto station = new StationDto(
				odhStationCode,
				externalStation.get("DEPIATA") + "-" + externalStation.get("DESTIATA") + "_" + externalStation.get("STD"),
				46.46248,
				11.32985
			);

			// station meta data
			Map<String, Object> metaData = new HashMap<>();

			List<String> keys = Arrays.asList(
				"ID", "DEP", "DEST", "STD", "STA", "ACFTAIL", "ATCEET", "ReleasedForDispatch",
				"TOA", "ATCID", "LatestFlightPlanDate", "Alt1", "Alt2", "ExternalFlightID", 
				"GUFI", "IsRecalc", "CustomReferences"
			);
			
			for (String key : keys) {
				metaData.put(key, externalStation.get(key));
			}

			station.setMetaData(metaData);

			station.setOrigin(odhClient.getProvenance().getLineage());

			odhStationList.add(station);

			// measurment
			DataMapDto<RecordDtoImpl> stationMeasurementsMap = rootMap.upsertBranch(odhStationCode);
			DataMapDto<RecordDtoImpl> typedStationMeasurements = stationMeasurementsMap.upsertBranch(DATATYPE_ID);

			DataMapDto<RecordDtoImpl> arrivalsMap = stationMeasurementsMap.upsertBranch("estimated-time-arrival");
			DataMapDto<RecordDtoImpl> departuresMap = stationMeasurementsMap.upsertBranch("estimated-time-departure");

			Instant instant = Instant.parse(externalStation.get("LastEditDate") + "Z");
			long timestamp = instant.toEpochMilli();
	
			List<RecordDtoImpl> values = typedStationMeasurements.getData();

			// specific eta measurment
			if(externalStation.containsKey("eta-local")) {
				SimpleRecordDto etaMeasurement = new SimpleRecordDto(timestamp, externalStation.get("eta-local"), period);
				values = arrivalsMap.getData();
				values.add(etaMeasurement);
			}

			// specific etd measurment
			if(externalStation.containsKey("etd-local")) {
				SimpleRecordDto etdMeasurement = new SimpleRecordDto(timestamp, externalStation.get("etd-local"), period);
				values = departuresMap.getData();
				values.add(etdMeasurement);
			}
		}

		// datatype
		List<DataTypeDto> odhDataTypeList = new ArrayList<>();

		odhDataTypeList.add(
			new DataTypeDto(
				"estimated-time-arrival",
				"ISO8601",
				"estimated real local arrival time",
				"deviation" 
			)
		);

		odhDataTypeList.add(
			new DataTypeDto(
				"estimated-time-departure",
				"ISO8601",
				"estimated real local departure time",
				"deviation" 
			)
		);

		try {
			odhClient.syncStations(odhStationList);
			odhClient.syncDataTypes(odhDataTypeList);

			pushMeasurments(corrections, rootMap);
		} catch (WebClientRequestException e) {
			LOG.error("Station & datatype sync failed: Request exception: {}", e.getMessage());
		}
    }

    public void pushMeasurments(List<Map<String,String>> corrections, DataMapDto<RecordDtoImpl> rootMap) {
        LOG.info("Pushing measurements for {}", odhClient.getIntegreenTypology());

		try {
			odhClient.pushData(rootMap);
			LOG.info("measurements push successful");
		} catch (WebClientRequestException e) {
			LOG.error("measurements push failed: Request exception: {}", e.getMessage());
		}
    }

}
