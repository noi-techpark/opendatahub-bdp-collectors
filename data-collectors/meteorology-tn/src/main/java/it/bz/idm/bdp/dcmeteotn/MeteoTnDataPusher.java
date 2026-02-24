// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcmeteotn;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import it.bz.idm.bdp.dcmeteotn.dto.MeteoTnDto;
import it.bz.idm.bdp.dcmeteotn.dto.MeteoTnMeasurementDto;
import it.bz.idm.bdp.dcmeteotn.dto.MeteoTnMeasurementListDto;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;

@Service
public class MeteoTnDataPusher extends NonBlockingJSONPusher {

    private static final Logger LOG = LoggerFactory.getLogger(MeteoTnDataPusher.class.getName());

    @Autowired
    private MeteoTnDataConverter converter;

    @Autowired
    private Environment env;

    public MeteoTnDataPusher() {
        LOG.debug("START.constructor.");
        LOG.debug("END.constructor.");
    }

    @Override
    public String initIntegreenTypology() {
        return "MeteoStation";
    }

    @Override
    public <T> DataMapDto<RecordDtoImpl> mapData(T rawData) {
        LOG.debug("START.mapData");

        @SuppressWarnings("unchecked")
        List<MeteoTnDto> data = (List<MeteoTnDto>) rawData;
        if (data == null) {
            return null;
        }

        DataMapDto<RecordDtoImpl> map = new DataMapDto<>();
        Date now = new Date();
        Long nowTimestamp = now.getTime();
        Integer period = converter.getPeriod();

        int countStations = 0;
        int countBranches = 0;
        int countMeasures = 0;

        for (MeteoTnDto dto : data) {

            removeAlreadyPushedData(dto);

            StationDto stationDto = dto.getStation();

            //Exclude Stations having endDate < today
            if ( dto.isValid() ) {

                String stationId = stationDto.getId();
                List<MeteoTnMeasurementListDto> measurementsByType = dto.getMeasurementsByType();

                //Check if we already treated this station (branch), if not found create the map and the list of records
                DataMapDto<RecordDtoImpl> recordsByStation = map.getBranch().get(stationId);
                if ( recordsByStation == null ) {
                    recordsByStation = new DataMapDto<>();
                    map.getBranch().put(stationId, recordsByStation);
                    List<RecordDtoImpl> dataList = new ArrayList<>();
                    recordsByStation.setData(dataList);
                    countStations++;
                }

                for (MeteoTnMeasurementListDto measurementListDto : measurementsByType) {
                    //String measurementListName = measurementListDto.getName();
                    List<MeteoTnMeasurementDto> measurements = measurementListDto.getMeasurements();

                    for (MeteoTnMeasurementDto measurementDto : measurements) {
                        //Get values from MeasutementDto and convert to SimpleRecordDto
                        String typeName = measurementDto.getTypeName();
                        long timestamp = measurementDto.getDate()!=null ? measurementDto.getDate().getTime() : nowTimestamp;
                        Object value = measurementDto.getValue();
                        
                        if(value == null) {
                            LOG.debug("IGNORE null value  MEASURE:  id="+stationDto.getId()+", typeName="+typeName);
                            continue;
                        }

                        SimpleRecordDto rec = new SimpleRecordDto();
                        rec.setValue(value);
                        rec.setTimestamp(timestamp);
                        rec.setPeriod(period);

                        //Check if we already treated this type (branch), if not found create the map and the list of records
                        DataMapDto<RecordDtoImpl> recordsByType = recordsByStation.getBranch().get(typeName);
                        if ( recordsByType == null ) {
                            recordsByType = new DataMapDto<>();
                            recordsByStation.getBranch().put(typeName, recordsByType);
                            List<RecordDtoImpl> dataList = new ArrayList<>();
                            recordsByType.setData(dataList);
                            countBranches++;
                        }

                        //Add the measure in the list
                        List<RecordDtoImpl> records = recordsByType.getData();
                        records.add(rec);
                        countMeasures++;
                        LOG.debug("ADD  MEASURE:  id="+stationDto.getId()+", typeName="+typeName+"  value="+value);
                    }
                }

            } else {
                LOG.debug("SKIP MEASURES: station_id="+stationDto.getId());
            }
        }

        LOG.debug("countStations="+countStations+"  countBranches="+countBranches+", countMeasures="+countMeasures);
        LOG.debug("map: "+map);
        LOG.debug("END.mapData");
        return map;
    }

    public <T> DataMapDto<RecordDtoImpl> mapSingleStationData2Bdp(MeteoTnDto data) {
        LOG.debug("START.mapSingleStationData2Bdp");
        if (data == null) {
            return null;
        }

        List<MeteoTnDto> list = new ArrayList<>();
        list.add(data);
        DataMapDto<RecordDtoImpl> map = mapData(list);

        LOG.debug("END.mapSingleStationData2Bdp");
        return map;
    }

    public StationList mapStations2Bdp(List<MeteoTnDto> data) {
        LOG.debug("START.mapStations2Bdp");
        if (data == null) {
            return null;
        }

        int countStations = 0;
        StationList stations = new StationList();
        for (MeteoTnDto dto : data) {
            StationDto stationDto = dto.getStation();
            Map<String, String> stationAttributes = dto.getStationAttributes();
            Object endDate = stationAttributes.get("enddate");

            //Exclude Stations having endDate<today
            if ( dto.isValid() ) {
                stations.add(stationDto);
                countStations++;
                LOG.debug("ADD  STATION:  id="+stationDto.getId()+", endDate="+endDate);
            } else {
                LOG.debug("SKIP STATION:  id="+stationDto.getId()+", endDate="+endDate);
            }
        }
        LOG.debug("countStations="+countStations+"  stations: "+stations);
        LOG.debug("END.mapStations2Bdp");
        return stations;
    }

    public List<DataTypeDto> mapDataTypes2Bdp(List<MeteoTnDto> data) {
        LOG.debug("START.mapdataTypes2Bdp");
        if (data == null) {
            return null;
        }

        List<DataTypeDto> dataTypeList = new ArrayList<>();
        Set<String> dataTypeNames = new HashSet<>();
        for (MeteoTnDto dto : data) {
            Map<String, DataTypeDto> dataTypes = dto.getDataTypes();
            Set<String> keySet = dataTypes!=null ? dataTypes.keySet() : null;

            if ( keySet!=null && !keySet.isEmpty() ) {
                for (String key : keySet) {
                    if ( !dataTypeNames.contains(key) ) {
                        DataTypeDto type = dataTypes.get(key);
                        dataTypeList.add(type);
                        dataTypeNames.add(key);
                        LOG.debug("ADD DataTypeDto="+type);
                    }
                }
            }

        }

        LOG.debug("dataTypeList: "+dataTypeList);
        LOG.debug("END.mapdataTypes2Bdp");
        return dataTypeList;
    }

    public Date getLastSavedRecordForStation(StationDto station) {
        LOG.debug("START.getLastSavedRecordForStation");
        if (station == null) {
            return null;
        }

        Date lastSavedRecord = null;
        Integer period = converter.getPeriod();
        String stationCode = station.getId();
        Object dateOfLastRecord = super.getDateOfLastRecord(stationCode, null, period);
        if ( dateOfLastRecord instanceof Date ) {
            lastSavedRecord = (Date) dateOfLastRecord;
        }

        LOG.debug("stationCode="+stationCode+"  lastSavedRecord="+lastSavedRecord);
        LOG.debug("END.getLastSavedRecordForStation");
        return lastSavedRecord;
    }

    public void removeAlreadyPushedData(MeteoTnDto meteoTnDto) {
        LOG.debug("START.removeAlreadyPushedData");
        if (meteoTnDto == null) {
            return;
        }

        //Exit if it is not necessary to read date of last saved record (check the environment parameter and the flag on the station)
        boolean envParamCheck = converter.isCheckDateOfLastRecord();
        boolean stationCheck = meteoTnDto.isCheckLastSavedRecord();
        if ( !stationCheck || !envParamCheck ) {
            return;
        }

        //Read date of last saved record, save it on the DTO for later use. Save also the information that we read the data to avoid unnecessary reads
        Date lastSavedRecord = meteoTnDto.getLastSavedRecord();
        if ( lastSavedRecord == null ) {
            StationDto station = meteoTnDto.getStation();
            lastSavedRecord = getLastSavedRecordForStation(station);
            meteoTnDto.setLastSavedRecord(lastSavedRecord);
            meteoTnDto.setCheckLastSavedRecord(false);
        }
        if ( lastSavedRecord != null ) {
            // Remove measurements older than lastSavedRecord in order to have less data to send to the Data Hub
            List<MeteoTnMeasurementListDto> measurementsByType = meteoTnDto.getMeasurementsByType();
            for ( int i=0 ; measurementsByType!=null && i<measurementsByType.size() ; i++ ) {
                MeteoTnMeasurementListDto measurementListDto = measurementsByType.get(i);
                List<MeteoTnMeasurementDto> measurements = measurementListDto.getMeasurements();
                List<MeteoTnMeasurementDto> filteredList = new ArrayList<>();
                MeteoTnMeasurementDto lastMeasurementDto = null;
                for ( int j=0 ; measurements!=null && j<measurements.size() ; j++ ) {
                    MeteoTnMeasurementDto measurementDto = measurements.get(j);
                    Date date = measurementDto.getDate();
                    if ( date.compareTo(lastSavedRecord) >= 0 ) {
                        filteredList.add(measurementDto);
                        lastMeasurementDto = measurementDto;
                    }
                }
                measurementListDto.setMeasurements(filteredList);
                if ( lastMeasurementDto!=null && LOG.isDebugEnabled() ) {
                    LOG.debug("LAST MEASURE: " + lastMeasurementDto);
                }
            }
        }

        LOG.debug("END.removeAlreadyPushedData");
    }

    @Override
    public String toString() {
        String str1 = "http://" + config.getString(HOST_KEY) + ":" + config.getString(PORT_KEY) + config.getString("json_endpoint");
        String str2 =
                "integreenTypology=" + this.integreenTypology   + "  " +
                "DEFAULT_HOST="      + DEFAULT_HOST     + "  " +
                "DEFAULT_PORT="      + DEFAULT_PORT     + "  " +
                "DEFAULT_ENDPOINT="  + DEFAULT_ENDPOINT + "  " +
                "";
        return str2 + " ---> " + str1;
    }

	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null,env.getProperty("provenance_name"), env.getProperty("provenance_version"), env.getProperty("app.origin"));
	}
}
