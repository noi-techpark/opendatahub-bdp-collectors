// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcmeteorologybz;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.HttpResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import it.bz.idm.bdp.dcmeteorologybz.dto.MeteorologyBzDto;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationList;

/**
 * Cronjob configuration can be found under src/main/resources/META-INF/spring/applicationContext.xml
 * XXX Do not forget to configure it!
 */
@Component
public class MeteorologyBzJobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorologyBzJobScheduler.class.getName());

    @Autowired
    private MeteorologyBzDataPusher pusher;

    @Autowired
    private MeteorologyBzDataRetriever retriever;

    @Autowired
    private MeteorologyBzDataConverter converter;

    /** JOB 1 */
    public void pushStations() throws Exception {
        LOG.info("START.pushStations");

        try {
            List<MeteorologyBzDto> data = retriever.fetchStations();

            StationList stations = pusher.mapStations2Bdp(data);
            if (stations != null) {
                pusher.syncStations(stations);
            }
        } catch (HttpClientErrorException e) {
            LOG.error(pusher + " - " + e + " - " + e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            LOG.error(pusher + " - " + e, e);
            throw e;
        }
        LOG.info("END.pushStations");
    }

    /** JOB 2 */
    public void pushDataTypes() throws Exception {
        LOG.info("START.pushDataTypes");

        try {
            List<DataTypeDto> dataTypes = retriever.fetchDataTypes(null);

            if (dataTypes != null){
                pusher.syncDataTypes(dataTypes);
            }

        } catch (HttpClientErrorException e) {
            LOG.error(pusher + " - " + e + " - " + e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            LOG.error(pusher + " - " + e, e);
            throw e;
        }
        LOG.info("END.pushDataTypes");
    }

    /** JOB 3 */
    public void pushData() throws Exception {
        LOG.info("START.pushData");

        try {

            boolean pushDataSingleStation = converter.isPushDataSingleStation();
            boolean checkDateOfLastRecord = converter.isCheckDateOfLastRecord();
            LOG.info("  pushData:  checkDateOfLastRecord="+checkDateOfLastRecord+"  pushDataSingleStation="+pushDataSingleStation);

            if ( pushDataSingleStation ) {

                //Fetch station data
                List<MeteorologyBzDto> data = retriever.fetchStations();
                int size = data!=null ? data.size() : 0;

                //fetch also DataTypes to fill sensor data list
                retriever.fetchDataTypes(data);

                //Send measurements separately for each station
                for (int i=0 ; i<size ; i++) {
                    MeteorologyBzDto meteoBzDto = data.get(i);
                    String stationId = meteoBzDto.getStation()!=null ? meteoBzDto.getStation().getId() : null;
                    try {
                        LOG.info("fetchData, "+i+" of "+size+": stationId="+stationId);

                        retriever.fetchDataByStation(meteoBzDto);

                        DataMapDto<RecordDtoImpl> stationRec = pusher.mapSingleStationData2Bdp(meteoBzDto);
                        stationRec.clean();
                        if (stationRec != null && !stationRec.getBranch().isEmpty()){
                            pusher.pushData(stationRec);
                        }
                    } catch (Exception ex) {
                        if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == 429) {
                            LOG.warn("pushData rate-limited, skipping station={}", stationId);
                        } else {
                            LOG.error("pushData error for station={}: {}", stationId, ex.getMessage(), ex);
                        }
                        continue;
                    }
                }

            } else {

                //Fetch all measurements
                List<MeteorologyBzDto> data = retriever.fetchData();

                //Push all measurements in a single call
                DataMapDto<RecordDtoImpl> stationRec = pusher.mapData(data);
                stationRec.clean();
                if (stationRec != null && !stationRec.getBranch().isEmpty()){
                    pusher.pushData(stationRec);
                }

            }

        } catch (HttpClientErrorException e) {
            LOG.error(pusher + " - " + e + " - " + e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            LOG.error(pusher + " - " + e, e);
            throw e;
        }
        LOG.info("END.pushData");
    }
}
