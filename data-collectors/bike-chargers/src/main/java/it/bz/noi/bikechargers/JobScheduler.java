// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.bikechargers;

import it.bz.idm.bdp.dto.*;
import it.bz.noi.bikechargers.configuration.BikeChargerConfiguration;
import it.bz.noi.bikechargers.configuration.DatatypeConfiguration;
import it.bz.noi.bikechargers.configuration.DatatypesConfiguration;
import it.bz.noi.bikechargers.model.BikeChargerBayStation;
import it.bz.noi.bikechargers.model.BikeChargerStation;
import it.bz.noi.bikechargers.model.BikeChargerStationDetails;
import it.bz.noi.bikechargers.pusher.AbstractBikeChargerJSONPusher;
import it.bz.noi.bikechargers.pusher.BikeChargerBayJSONPusher;
import it.bz.noi.bikechargers.pusher.BikeChargerJSONPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

@Component
public class JobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

    @Autowired
    private BikeChargerConnector bikeChargerConnector;
    @Autowired
    private BikeChargerConfiguration bikeChargerConfiguration;
    @Autowired
    private DatatypesConfiguration datatypesConfiguration;

    @Autowired
    private BikeChargerJSONPusher bikeChargerJSONPusher;
    @Autowired
    private BikeChargerBayJSONPusher bikeChargerBayJSONPusher;

    @PostConstruct
    private void syncDataTypes() {
        setupDataType(bikeChargerJSONPusher, datatypesConfiguration.getAllBikeChargerDataTypes());
        setupDataType(bikeChargerBayJSONPusher, datatypesConfiguration.getAllBikeChargerBayDataTypes());
    }

    @Scheduled(cron = "${scheduler.cron}")
    public void execute() {
        LOG.info("MainBikeCharger execute");
        try {

            List<BikeChargerStation> stations = bikeChargerConnector.getStations();
            LOG.debug("got {} stations", stations.size());

            StationList bikeChargerStationList = new StationList();
            StationList bikeChargerBayStationList = new StationList();
            DataMapDto<RecordDtoImpl> bikeChargerDataMap = new DataMapDto<>();
            DataMapDto<RecordDtoImpl> bikeChargerBayDataMap = new DataMapDto<>();

            for (BikeChargerStation station : stations) {
                StationDto stationDto = new StationDto(station.getId(),
                        station.getName(),
                        station.getLat(),
                        station.getLng());
                stationDto.setOrigin(bikeChargerConfiguration.getOrigin());
                stationDto.setStationType(bikeChargerConfiguration.getBikeChargerStationtype());
                stationDto.getMetaData().put("address", station.getAddress());

                BikeChargerStationDetails bikeChargerStationDetails = bikeChargerConnector.getStationDetails(station);
                stationDto.getMetaData().put("totalBays", bikeChargerStationDetails.getTotalBays());

                bikeChargerDataMap.addRecord(station.getId(), datatypesConfiguration.getState().getKey(),
                        new SimpleRecordDto(System.currentTimeMillis(), station.getState(),
                                bikeChargerConfiguration.getPeriod()));
                bikeChargerDataMap.addRecord(station.getId(), datatypesConfiguration.getFreebay().getKey(),
                        new SimpleRecordDto(System.currentTimeMillis(), bikeChargerStationDetails.getFreeBay(),
                                bikeChargerConfiguration.getPeriod()));
                bikeChargerDataMap.addRecord(station.getId(), datatypesConfiguration.getAvailableVehicles().getKey(),
                        new SimpleRecordDto(System.currentTimeMillis(),
                                bikeChargerStationDetails.getAvailableVehicles(),
                                bikeChargerConfiguration.getPeriod()));

                LOG.debug("got {} bays", bikeChargerStationDetails.getBayStations().size());

                for (BikeChargerBayStation bayStation : bikeChargerStationDetails.getBayStations()) {
                    StationDto bayStationDto = new StationDto(
                            bikeChargerConfiguration.getOrigin() + ":" + bayStation.getLabel(),
                            bayStation.getLabel(),
                            station.getLat(),
                            station.getLng());
                    bayStationDto.setParentStation(station.getId());
                    bayStationDto.setOrigin(bikeChargerConfiguration.getOrigin());
                    bayStationDto.setStationType(bikeChargerConfiguration.getBikeChargerBayStationtype());
                    bayStationDto.getMetaData().put("charger", bayStation.getCharger());
                    bayStationDto.getMetaData().put("use", bayStation.getUse());

                    bikeChargerBayDataMap.addRecord(bayStationDto.getId(), datatypesConfiguration.getState().getKey(),
                            new SimpleRecordDto(System.currentTimeMillis(), bayStation.getState(),
                                    bikeChargerConfiguration.getPeriod()));
                    bikeChargerBayDataMap.addRecord(bayStationDto.getId(),
                            datatypesConfiguration.getUsageState().getKey(),
                            new SimpleRecordDto(System.currentTimeMillis(), bayStation.getUsageState(),
                                    bikeChargerConfiguration.getPeriod()));

                    bikeChargerBayStationList.add(bayStationDto);
                }

                bikeChargerStationList.add(stationDto);
            }
            bikeChargerJSONPusher.syncStations(bikeChargerStationList);
            bikeChargerJSONPusher.pushData(bikeChargerDataMap);
            bikeChargerBayJSONPusher.syncStations(bikeChargerBayStationList);
            bikeChargerBayJSONPusher.pushData(bikeChargerBayDataMap);
        } catch (Exception e) {
            LOG.error("reading stations failed ", e);
        }
    }

    private static void setupDataType(AbstractBikeChargerJSONPusher jsonPusher,
            List<DatatypeConfiguration> datatypeConfigurations) {
        List<DataTypeDto> dataTypeDtoList = new ArrayList<>();
        for (DatatypeConfiguration datatypeConfiguration : datatypeConfigurations) {
            dataTypeDtoList.add(
                    new DataTypeDto(datatypeConfiguration.getKey(),
                            datatypeConfiguration.getUnit(),
                            datatypeConfiguration.getDescription(),
                            datatypeConfiguration.getRtype()));
        }
        jsonPusher.syncDataTypes(dataTypeDtoList);
    }
}
