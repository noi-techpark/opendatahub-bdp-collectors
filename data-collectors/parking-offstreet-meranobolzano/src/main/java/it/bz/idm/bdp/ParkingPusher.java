// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationList;
import it.bz.idm.bdp.forecast.domain.ParkingForecasts;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;
import it.bz.idm.bdp.metadata.MetadataEnrichment;
import it.bz.idm.bdp.util.MappingUtil;

@Component
public class ParkingPusher extends NonBlockingJSONPusher{

	private static final String MUNICIPALITY_MERANO = "Municipality Merano";

	private static final String STATION_TYPE = "ParkingStation";

	private static final String PARKINGSLOT_TYPEIDENTIFIER = "occupied";

	private static final String PARKINGSLOT_METRIC = "Count";

	private static final String TYPE_UNIT = "";

	private static final String FORECAST_METRIC = "Forecast";

	private static final String TYPEDESCRIPTION_SUFFIX = " minutes forecast";

	public static final String FORECAST_PREFIX = "parking-forecast-";

	public static final int[] PREDICTION_FORECAST_TIMES_IN_MINUTES = {30,60,90,120,150,180,210,240};

	@Autowired
	private ParkingMeranoClient parkingMeranoClient;

	@Autowired
	private PredictionRetriever predictionRetriever;
	@Autowired
	private ParkingFrontEndRetriever parkingFrontEndRetriever;

	@Autowired
	private Environment env;

	@Lazy
    @Autowired
	private MetadataEnrichment metadataEnrichment;

	public void pushParkingMetaData() throws IOException {
		StationList stations = new StationList();

		StationList meStations = new StationList();
		parkingMeranoClient.insertParkingMetaDataInto(meStations);
		// Workaround to maintain metadata syncing while endpoint is not reachable
		// In that case just use the stations as they are in the Open Data Hub
		if (meStations.isEmpty()) {
			meStations.addAll(fetchStations(this.integreenTypology, parkingMeranoClient.getOrigin()));
		}
		stations.addAll(meStations);

		// metadata enrichment
		metadataEnrichment.mapData(stations);

		if (!stations.isEmpty())
			syncStations(this.integreenTypology,stations);
	}


	public void pushPredictionData(){
		String[] identifers = parkingFrontEndRetriever.getActiveStationIdentifers();
		if (identifers!=null){
			connectToDataCenterCollector();
			DataMapDto<RecordDtoImpl> dataMap = new DataMapDto<>();
			for (String stationIdentifier:identifers){
				ParkingForecasts predictions = predictionRetriever.predict(stationIdentifier);
				DataMapDto<RecordDtoImpl> typeMap = MappingUtil.generateTypeMap(predictions);
				dataMap.getBranch().put(stationIdentifier, typeMap);
			}
			pushData(dataMap);
		}
	}

	public void pushData() {
		DataMapDto<RecordDtoImpl> meranoDataMap = new DataMapDto<RecordDtoImpl>();

		this.provenance = new ProvenanceDto(null, env.getProperty("provenance_name"), env.getProperty("provenance_version"), MUNICIPALITY_MERANO);;
		parkingMeranoClient.insertDataInto(meranoDataMap);
		pushData(meranoDataMap);
	}


	@Override
	public String initIntegreenTypology() {
		return STATION_TYPE;
	}


	@Override
	public <T> DataMapDto<RecordDtoImpl> mapData(T data) {
		return null;
	}


	public static List<DataTypeDto> getDataTypeList() {
		List<DataTypeDto> dataTypes = new ArrayList<>();
		dataTypes.add(new DataTypeDto(PARKINGSLOT_TYPEIDENTIFIER,TYPE_UNIT,"Occupacy of a parking area",PARKINGSLOT_METRIC));
		for (int minutesForecast:PREDICTION_FORECAST_TIMES_IN_MINUTES)
			dataTypes.add(new DataTypeDto(FORECAST_PREFIX+ minutesForecast,TYPE_UNIT,minutesForecast+TYPEDESCRIPTION_SUFFIX,FORECAST_METRIC));
		return dataTypes;
	}


	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null, env.getProperty("provenance_name"), env.getProperty("provenance_version"), env.getProperty("pbz_origin"));
	}
}
