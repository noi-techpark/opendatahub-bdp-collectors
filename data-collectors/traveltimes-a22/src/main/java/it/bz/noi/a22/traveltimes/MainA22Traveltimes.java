// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22.traveltimes;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;

@Component
@Configuration
@PropertySource("classpath:it/bz/noi/a22/traveltimes/a22connector.properties")
public class MainA22Traveltimes
{
	@Value("${a22url}")
    private String a22ConnectorURL;

    @Value("${a22user}")
    private String a22ConnectorUsr;

    @Value("${a22password}")
    private String a22ConnectorPwd;

	private static final Logger LOG = LoggerFactory.getLogger(MainA22Traveltimes.class);
	private static final String TVCC_STATION_ID_PREFIX = "urn:linkstation:a22:tvcc:";

	private final A22Properties datatypesProperties;
	private final A22Properties a22TraveltimesProperties;
	@Autowired
	A22TraveltimesJSONPusher pusher;

	public MainA22Traveltimes() {
		this.datatypesProperties = new A22Properties("a22traveltimesdatatypes.properties");
		this.a22TraveltimesProperties = new A22Properties("a22traveltimes.properties");
	}

	public void execute()
	{
		long startTime = System.currentTimeMillis();
		try
		{
			LOG.info("Start MainA22Traveltimes");

			// step 1
			// create a Connector instance: this will perform authentication and store the session
			//
			// the session will last 24 hours unless de-authenticated before - however, if a user
			// de-authenticates one session, all sessions of the same user will be de-authenticated
			Connector a22Service = setupA22ServiceConnector();

			setupDataType();

			// step 2
			// get the list of segments (regular + TVCC) and sync all stations at once
			StationList regularStationList = new StationList();
			StationList tvccStationList = new StationList();
			try {
				// step 2a: regular segments
				List<HashMap<String, String>> segments = a22Service.getTravelTimeSegments();
				LOG.debug("got " + segments.size() + " regular segments");
				if (!segments.isEmpty()) {
					LOG.debug("the first regular segment is: {}", segments.get(0));
					segments.forEach(segment -> {
						StationDto edge = new StationDto(segment.get("idtratto"),
								segment.get("descrizione"),
								null,
								null);
						edge.setOrigin(a22TraveltimesProperties.getProperty("origin"));
						edge.setStationType(a22TraveltimesProperties.getProperty("stationtype"));
						addSegmentMetadata(edge, segment);
						edge.getMetaData().put("dataset", "tollgate");
						regularStationList.add(edge);
					});
				}

				// step 2b: TVCC segments
				List<HashMap<String, String>> tvccSegments = a22Service.getTVCCSegments();
				LOG.debug("got " + tvccSegments.size() + " TVCC segments");
				if (!tvccSegments.isEmpty()) {
					LOG.debug("the first TVCC segment is: {}", tvccSegments.get(0));
					tvccSegments.forEach(segment -> {
						String rawIdtratto = segment.get("idtratto");
						StationDto edge = new StationDto(TVCC_STATION_ID_PREFIX + rawIdtratto,
								segment.get("descrizione"),
								null,
								null);
						edge.setOrigin(a22TraveltimesProperties.getProperty("origin"));
						edge.setStationType(a22TraveltimesProperties.getProperty("stationtype"));
						addSegmentMetadata(edge, segment);
						edge.getMetaData().put("dataset", "tvcc");
						edge.getMetaData().put("provider_id", rawIdtratto);
						if (segment.containsKey("descrizioneD")) {
							edge.getMetaData().put("descrizioneD", segment.get("descrizioneD"));
						}
						tvccStationList.add(edge);
					});
				}

				// step 2c: merge and sync all stations at once
				StationList combinedStationList = new StationList();
				combinedStationList.addAll(regularStationList);
				combinedStationList.addAll(tvccStationList);
				if (!combinedStationList.isEmpty()) {
					pusher.syncStations(pusher.initIntegreenTypology(), combinedStationList);
				}
			} catch (Exception e) {
				LOG.error("step 2 failed, continuing anyway to de-auth...", e);
			}

			// step 3
			// get the list of regular travel times
			try {
				long scanWindowSeconds = Long.parseLong(a22TraveltimesProperties.getProperty("scanWindowSeconds"));
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				for (int i = 0; i < regularStationList.size(); i++) {
					String id = regularStationList.get(i).getId();

					long lastTimeStamp = getLastTimestampOfStationInSeconds(id);

					do {
						DataMapDto<RecordDtoImpl> recs = new DataMapDto<>();

						List<HashMap<String, String>> traveltimes = a22Service.getTravelTimes(lastTimeStamp, lastTimeStamp + scanWindowSeconds, id);

						LOG.debug("got " + traveltimes.size() + " traveltime data records for " + simpleDateFormat.format(new Date(lastTimeStamp * 1000)) + ", " + simpleDateFormat.format(new Date((lastTimeStamp + scanWindowSeconds) * 1000)) + ", " + id + ":");
						if (!traveltimes.isEmpty()) {
							LOG.debug("the first travel time is: {}", traveltimes.get(0));
							traveltimes.forEach(traveltime -> {
								try{
									mapTravelTimeRecord(recs, traveltime.get("idtratto"), traveltime, false);
								} catch (Exception e) {
									LOG.error("Error during traveltime elaboration. Dumping current traveltime data structure:", e);
									LOG.error(traveltime.toString());
									throw e;
								}
							});

							LOG.debug("pushing regular data: " + regularStationList.size() + " stations");
							pusher.pushData(recs);
						}
						lastTimeStamp += scanWindowSeconds;
					} while (lastTimeStamp < System.currentTimeMillis() / 1000);
				}

			} catch (Exception e) {
				LOG.error("step 3 failed, continuing anyway...", e);
			}

			// step 4
			// get the list of TVCC travel times
			try {
				long scanWindowSeconds = Long.parseLong(a22TraveltimesProperties.getProperty("scanWindowSeconds"));
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				for (int i = 0; i < tvccStationList.size(); i++) {
					String stationId = tvccStationList.get(i).getId();
					String rawIdtratto = (String) tvccStationList.get(i).getMetaData().get("provider_id");

					long lastTimeStamp = getLastTimestampOfStationInSeconds(stationId);

					do {
						DataMapDto<RecordDtoImpl> recs = new DataMapDto<>();

						List<HashMap<String, String>> traveltimes = a22Service.getTVCCTravelTimes(lastTimeStamp, lastTimeStamp + scanWindowSeconds, rawIdtratto);

						LOG.debug("got " + traveltimes.size() + " TVCC traveltime data records for " + simpleDateFormat.format(new Date(lastTimeStamp * 1000)) + ", " + simpleDateFormat.format(new Date((lastTimeStamp + scanWindowSeconds) * 1000)) + ", " + stationId + ":");
						if (!traveltimes.isEmpty()) {
							LOG.debug("the first TVCC travel time is: {}", traveltimes.get(0));
							traveltimes.forEach(traveltime -> {
								try{
									mapTravelTimeRecord(recs, stationId, traveltime, true);
								} catch (Exception e) {
									LOG.error("Error during TVCC traveltime elaboration. Dumping current traveltime data structure:", e);
									LOG.error(traveltime.toString());
									throw e;
								}
							});

							LOG.debug("pushing TVCC data: " + tvccStationList.size() + " stations");
							pusher.pushData(recs);
						}
						lastTimeStamp += scanWindowSeconds;
					} while (lastTimeStamp < System.currentTimeMillis() / 1000);
				}

			} catch (Exception e) {
				LOG.error("step 4 (TVCC) failed, continuing anyway to de-auth...", e);
			}

			// step 5
			// de-authentication
			a22Service.close();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		finally
		{
			long stopTime = System.currentTimeMillis();
			LOG.debug("elaboration time (millis): " + (stopTime - startTime));
		}
	}

	private Connector setupA22ServiceConnector() throws IOException
	{
		return new Connector(a22ConnectorURL, a22ConnectorUsr, a22ConnectorPwd);
	}
	
	private DataTypeDto mkDt(String id, String postix) {
		return new DataTypeDto(datatypesProperties.getProperty("a22traveltimes.datatype."+id+".key") + postix,
				datatypesProperties.getProperty("a22traveltimes.datatype."+id+".unit"),
				datatypesProperties.getProperty("a22traveltimes.datatype."+id+".description"),
				datatypesProperties.getProperty("a22traveltimes.datatype."+id+".rtype"));
	}

	private void setupDataType() {
		List<DataTypeDto> dataTypeDtoList = new ArrayList<>();

		dataTypeDtoList.add(mkDt("lds_leggeri", ""));
		dataTypeDtoList.add(mkDt("lds_leggeri", "_desc"));
		dataTypeDtoList.add(mkDt("lds_leggeri", "_val"));
		dataTypeDtoList.add(mkDt("velocita_leggeri", ""));
		dataTypeDtoList.add(mkDt("tempo_leggeri", ""));

		dataTypeDtoList.add(mkDt("lds_pesanti", ""));
		dataTypeDtoList.add(mkDt("lds_pesanti", "_desc"));
		dataTypeDtoList.add(mkDt("lds_pesanti", "_val"));
		dataTypeDtoList.add(mkDt("velocita_pesanti", ""));
		dataTypeDtoList.add(mkDt("tempo_pesanti", ""));

		dataTypeDtoList.add(mkDt("num_leggeri", ""));
		dataTypeDtoList.add(mkDt("num_pesanti", ""));

		pusher.syncDataTypes(dataTypeDtoList);
	}

	private void addSegmentMetadata(StationDto edge, HashMap<String, String> segment) {
		edge.getMetaData().put("latitudineinizio", Double.parseDouble(segment.get("latitudineinizio")));
		edge.getMetaData().put("longitudininizio", Double.parseDouble(segment.get("longitudininizio")));
		edge.getMetaData().put("latitudinefine", Double.parseDouble(segment.get("latitudinefine")));
		edge.getMetaData().put("longitudinefine", Double.parseDouble(segment.get("longitudinefine")));

		String idDirezione = "";
		switch(Integer.parseInt(segment.get("iddirezione"))) {
			case 1:
				idDirezione = "Sud";
				break;
			case 2:
				idDirezione = "Nord";
				break;
			case 3:
				idDirezione = "Entrmbe";
				break;
			default:
				idDirezione = "Non definito";
				break;
		}
		edge.getMetaData().put("iddirezione", idDirezione);
		edge.getMetaData().put("metroinizio", Integer.parseInt(segment.get("metroinizio")));
		edge.getMetaData().put("metrofine", Integer.parseInt(segment.get("metrofine")));
		edge.getMetaData().put("lunghezza", Math.abs(Integer.parseInt(segment.get("metrofine")) - Integer.parseInt(segment.get("metroinizio"))));
	}

	private void mapTravelTimeRecord(DataMapDto<RecordDtoImpl> recs, String stationId, HashMap<String, String> traveltime, boolean includeCounts) {
		long ts = Long.parseLong(traveltime.get("data")) * 1000;

		// ########## LIGHT VEHICLES ##########
		// lds
		String ldsLightKey = datatypesProperties.getProperty("a22traveltimes.datatype.lds_leggeri.key");
		String ldsLightRaw = traveltime.get("lds");
		recs.addRecord(stationId, ldsLightKey, new SimpleRecordDto(ts, ldsLightRaw, 1));
		String ldsLightDesc = datatypesProperties.getProperty("a22traveltimes.datatype.lds_leggeri.mapping." + ldsLightRaw + ".desc");
		recs.addRecord(stationId, ldsLightKey + "_desc", new SimpleRecordDto(ts, ldsLightDesc, 1));
		Double ldsLightVal = Double.parseDouble(datatypesProperties.getProperty("a22traveltimes.datatype.lds_leggeri.mapping." + ldsLightRaw + ".val"));
		recs.addRecord(stationId, ldsLightKey + "_val", new SimpleRecordDto(ts, ldsLightVal, 1));

		// tempo
		String tempoLightKey = datatypesProperties.getProperty("a22traveltimes.datatype.tempo_leggeri.key");
		recs.addRecord(stationId, tempoLightKey, new SimpleRecordDto(ts, Double.parseDouble(traveltime.get("tempo")), 1));
		// velocita
		String velocitaLightKey = datatypesProperties.getProperty("a22traveltimes.datatype.velocita_leggeri.key");
		recs.addRecord(stationId, velocitaLightKey, new SimpleRecordDto(ts, Double.parseDouble(traveltime.get("velocita")), 1));

		// ########## HEAVY VEHICLES ##########
		// lds
		String ldsHeavyKey = datatypesProperties.getProperty("a22traveltimes.datatype.lds_pesanti.key");
		String ldsHeavyRaw = traveltime.get("pesanti_lds");
		recs.addRecord(stationId, ldsHeavyKey, new SimpleRecordDto(ts, ldsHeavyRaw, 1));
		String ldsHeavyDesc = datatypesProperties.getProperty("a22traveltimes.datatype.lds_pesanti.mapping." + ldsHeavyRaw + ".desc");
		recs.addRecord(stationId, ldsHeavyKey + "_desc", new SimpleRecordDto(ts, ldsHeavyDesc, 1));
		Double ldsHeavyVal = Double.parseDouble(datatypesProperties.getProperty("a22traveltimes.datatype.lds_pesanti.mapping." + ldsHeavyRaw + ".val"));
		recs.addRecord(stationId, ldsHeavyKey + "_val", new SimpleRecordDto(ts, ldsHeavyVal, 1));

		// tempo
		String tempoHeavyKey = datatypesProperties.getProperty("a22traveltimes.datatype.tempo_pesanti.key");
		recs.addRecord(stationId, tempoHeavyKey, new SimpleRecordDto(ts, Double.parseDouble(traveltime.get("pesanti_tempo")), 1));
		// velocita
		String velocitaHeavyKey = datatypesProperties.getProperty("a22traveltimes.datatype.velocita_pesanti.key");
		recs.addRecord(stationId, velocitaHeavyKey, new SimpleRecordDto(ts, Double.parseDouble(traveltime.get("pesanti_velocita")), 1));

		// ########## VEHICLE COUNTS (TVCC only) ##########
		if (includeCounts) {
			String numLeggeriKey = datatypesProperties.getProperty("a22traveltimes.datatype.num_leggeri.key");
			recs.addRecord(stationId, numLeggeriKey, new SimpleRecordDto(ts, Double.parseDouble(traveltime.get("num_leggeri")), 1));
			String numPesantiKey = datatypesProperties.getProperty("a22traveltimes.datatype.num_pesanti.key");
			recs.addRecord(stationId, numPesantiKey, new SimpleRecordDto(ts, Double.parseDouble(traveltime.get("num_pesanti")), 1));
		}
	}

	private long getLastTimestampOfStationInSeconds(String stationId) {
		try {
			long defaultTs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.parse(a22TraveltimesProperties.getProperty("lastTimestamp")).getTime();

			String dataType = datatypesProperties.getProperty("a22traveltimes.datatype.lds_leggeri.key");
			long ret = ((Date) pusher.getDateOfLastRecord(stationId, dataType, null)).getTime();

			// Use default time as latest starting point. Remote API might not have data before that time
			ret = Math.max(ret, defaultTs);

			LOG.debug("getLastTimestampOfStationInSeconds(" + stationId + "): " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ret));

			return ret / 1000;
		} catch (ParseException e) {
			throw new RuntimeException("Invalid lastTimestamp: " + a22TraveltimesProperties.getProperty("lastTimestamp"), e);
		}
	}

	/*
	 * Method used only for development/debugging
	 */
	public static void main(String[] args)
	{
		new MainA22Traveltimes().execute();
	}

}
