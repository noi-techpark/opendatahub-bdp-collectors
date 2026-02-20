// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import it.bz.idm.bdp.dto.*;
import it.bz.noi.ondemandmerano.configuration.DatatypeConfiguration;
import it.bz.noi.ondemandmerano.configuration.DatatypesConfiguration;
import it.bz.noi.ondemandmerano.configuration.OnDemandMeranoConfiguration;
import it.bz.noi.ondemandmerano.model.OnDemandMeranoActivity;
import it.bz.noi.ondemandmerano.model.OnDemandMeranoPolygon;
import it.bz.noi.ondemandmerano.model.OnDemandMeranoStop;
import it.bz.noi.ondemandmerano.model.OnDemandMeranoVehicle;
import it.bz.noi.ondemandmerano.pusher.ItineraryJSONPusher;
import it.bz.noi.ondemandmerano.pusher.PolygonJSONPusher;
import it.bz.noi.ondemandmerano.pusher.StopJSONPusher;
import it.bz.noi.ondemandmerano.pusher.VehicleJSONPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MainOnDemandMerano {

    private static final Logger LOG = LoggerFactory.getLogger(MainOnDemandMerano.class);

    @Autowired
    private OnDemandMeranoConnector onDemandMeranoConnector;
    @Autowired
    private OnDemandMeranoConfiguration onDemandMeranoConfiguration;
    @Autowired
    private DatatypesConfiguration datatypesConfiguration;

    @Autowired
    private StopJSONPusher stopJSONPusher;
    @Autowired
    private PolygonJSONPusher polygonJSONPusher;
    @Autowired
    private VehicleJSONPusher vehicleJSONPusher;
    @Autowired
    private ItineraryJSONPusher itineraryJSONPusher;

    public void executeStops() {
        LOG.info("MainOnDemandService executeStops");

        try {
            List<OnDemandMeranoStop> stops = onDemandMeranoConnector.getStops();
            LOG.debug("got {} stops", stops.size());
            StationList stopsStationList = new StationList();
            for (OnDemandMeranoStop stop : stops) {
                StationDto stationDto = new StationDto(onDemandMeranoConfiguration.getOrigin() + ":" + stop.getId(),
                        stop.getTitle(),
                        stop.getPosition().getLatitude(),
                        stop.getPosition().getLongitude());
                stationDto.setOrigin(onDemandMeranoConfiguration.getOrigin());
                stationDto.setStationType(onDemandMeranoConfiguration.getStopsStationtype());
                stationDto.getMetaData().put("address", stop.getAddress());
                stationDto.getMetaData().put("groups", stop.getGroups());
                stationDto.getMetaData().put("region", stop.getRegion());
                stationDto.getMetaData().put("reference", stop.getReference());
                stationDto.getMetaData().put("type", stop.getType());
                stopsStationList.add(stationDto);
            }
            stopJSONPusher.syncStations(stopsStationList);
        } catch (Exception e) {
            LOG.error("reading stops failed ", e);
        }
    }

    public void executePolygons() {
        LOG.info("MainOnDemandService executePolygons");

        try {
            List<OnDemandMeranoPolygon> polygons = onDemandMeranoConnector.getPolygons();
            LOG.debug("got {} polygons", polygons.size());
            List<EventDto> eventDtoList = new ArrayList<EventDto>();
            for (OnDemandMeranoPolygon polygon : polygons) {
                EventDto eventDto = new EventDto();
                String uuidNode = Integer.toHexString(polygon.getId());
                String uuid = onDemandMeranoConfiguration.getPolygonUuidPrefix() + "-" + String.join("", Collections.nCopies(12 - uuidNode.length(), "0")) + uuidNode;
                eventDto.setUuid(UUID.fromString(uuid).toString());
                eventDto.setDescription(polygon.getName());
                eventDto.setOrigin(onDemandMeranoConfiguration.getOrigin());
                eventDto.setCategory(onDemandMeranoConfiguration.getPolygonsCategory());

                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                List<List<Double>> polygonCoordinates = polygon.getGeometry().getCoordinates().get(0);
                Coordinate[] coordinates = new Coordinate[polygonCoordinates.size()];
                for(int i = 0; i < coordinates.length; i++) {
                    List<Double> c = polygonCoordinates.get(i);
                    coordinates[i] = new Coordinate(c.get(0), c.get(1));
                }
                LOG.debug("polygon: {}", geometryFactory.createPolygon(coordinates).toText());
                eventDto.setWktGeometry(geometryFactory.createPolygon(coordinates).toText());

                eventDtoList.add(eventDto);
            }
            polygonJSONPusher.addEvents(eventDtoList);
        } catch (Exception e) {
            LOG.error("reading stops failed ", e);
        }
    }

    public void executeActivities() {
        LOG.debug("MainOnDemandService executeActivities");
        try {
            setupDataType();

            List<OnDemandMeranoActivity> activites = onDemandMeranoConnector.getActivities();
            LOG.debug("got {} activites", activites.size());

            try {
                elaborateVehicles(activites.stream().map(OnDemandMeranoActivity::getVehicle).collect(Collectors.toList()));
            } catch (Exception e) {
                LOG.error("elaborate vehicles failed", e);
            }

            try {
                elaborateIternaties(activites);
            } catch (Exception e) {
                LOG.error("elaborate itinerary failed", e);
            }
        } catch (Exception e) {
            LOG.error("reading activite failed", e);
        }

    }

    private void elaborateVehicles(List<OnDemandMeranoVehicle> vehicles) throws Exception {
        LOG.debug("MainOnDemandService elaborateVehicles");
        List<StationDto> preStations = vehicleJSONPusher.fetchStations(onDemandMeranoConfiguration.getVehiclesStationtype(),
                onDemandMeranoConfiguration.getOrigin());
        StationList stopsStationList = new StationList();
        DataMapDto<RecordDtoImpl> dataMap = new DataMapDto<>();
        int positionMeasurementCount = 0;
        for (OnDemandMeranoVehicle vehicle : vehicles) {
            StationDto stationDto = new StationDto(vehicle.getLicensePlateNumber(),
                    vehicle.getLicensePlateNumber(),
                    vehicle.getPosition().getLatitude(),
                    vehicle.getPosition().getLongitude());
            stationDto.setOrigin(onDemandMeranoConfiguration.getOrigin());
            stationDto.setStationType(onDemandMeranoConfiguration.getVehiclesStationtype());
            stationDto.getMetaData().put("type", vehicle.getType());
            stationDto.getMetaData().put("operator", vehicle.getOperator());
            stationDto.getMetaData().put("capacityMax", vehicle.getCapacityMax());
            stationDto.getMetaData().put("capacityUsed", vehicle.getCapacityUsed());
            stopsStationList.add(stationDto);

            StationDto preStation = preStations.stream().filter(s -> s.getId().equals(stationDto.getId())).findFirst().orElseGet(() -> null);
            boolean updatePosition = true;
            if (preStation != null
                    && preStation.getLatitude() != null
                    && preStation.getLongitude() != null
                    && stationDto.getLatitude() != null
                    && stationDto.getLongitude() != null) {
                double distance = calcDistanceBetweenStations(stationDto, preStation);
                if (distance < 10) {
                    updatePosition = false;
                    // reset stations position
                    stationDto.setLatitude(preStation.getLatitude());
                    stationDto.setLongitude(preStation.getLongitude());
                }
            }

            if (updatePosition) {
                String recordTime = vehicle.getRecordTime();
                ZonedDateTime recordTimeZonedDateTime = ZonedDateTime.parse(recordTime);
                long recordTimeLong = recordTimeZonedDateTime.toInstant().toEpochMilli();

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> positionMap = mapper.convertValue(vehicle.getPosition(), new TypeReference<Map<String, Object>>() {});

                dataMap.addRecord(vehicle.getLicensePlateNumber(), datatypesConfiguration.getPosition().getKey(),
                        new SimpleRecordDto(recordTimeLong, positionMap, onDemandMeranoConfiguration.getVehiclesPeriod()));
                positionMeasurementCount++;
            }
        }
        vehicleJSONPusher.syncStations(stopsStationList);
        if (positionMeasurementCount > 0) {
            vehicleJSONPusher.pushData(dataMap);
        }
    }

    private void elaborateIternaties(List<OnDemandMeranoActivity> activites) throws Exception {
        LOG.debug("MainOnDemandService elaborateIternaties");

        StationList itineraryStationList = new StationList();
        DataMapDto<RecordDtoImpl> dataMap = new DataMapDto<>();

        for (OnDemandMeranoActivity activity : activites) {
            StationDto stationDto = new StationDto();
            stationDto.setId(activity.getId().toString());
            stationDto.setName(activity.getPlannedStartAt().toLocalDate().toString());
            stationDto.setOrigin(onDemandMeranoConfiguration.getOrigin());
            stationDto.setStationType(onDemandMeranoConfiguration.getItineraryStationtype());

            itineraryStationList.add(stationDto);

            long recordTimeLong = activity.getUpdatedAt().toInstant().toEpochMilli();

            LOG.debug("dataMap.addRecord ({}, {}): {}", stationDto.getId(), datatypesConfiguration.getItineraryDetails().getKey(),
                    new GsonBuilder().create().toJson(activity.toJson()));
            dataMap.addRecord(stationDto.getId(), datatypesConfiguration.getItineraryDetails().getKey(),
                    new SimpleRecordDto(recordTimeLong, activity.toJson(), onDemandMeranoConfiguration.getItineraryPeriod()));
        }
        itineraryJSONPusher.syncStations(itineraryStationList);
        itineraryJSONPusher.pushData(dataMap);
    }


    private void setupDataType() {
        List<DataTypeDto> dataTypeDtoList = new ArrayList<>();
        for (DatatypeConfiguration datatypeConfiguration : this.datatypesConfiguration.getAllDataTypes()) {
            dataTypeDtoList.add(
                    new DataTypeDto(datatypeConfiguration.getKey(),
                            datatypeConfiguration.getUnit(),
                            datatypeConfiguration.getDescription(),
                            datatypeConfiguration.getRtype())
            );
        }
        vehicleJSONPusher.syncDataTypes(dataTypeDtoList);
    }

    public static double calcDistanceBetweenStations(StationDto s1, StationDto s2) {
        double lon1 = Math.toRadians(s1.getLongitude());
        double lon2 = Math.toRadians(s2.getLongitude());
        double lat1 = Math.toRadians(s1.getLatitude());
        double lat2 = Math.toRadians(s2.getLatitude());

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        double r = 6.371 * Math.pow(10, 6);

        return (c * r);
    }

}
