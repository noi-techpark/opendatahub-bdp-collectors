// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.bz.noi.ondemandmerano.configuration.ConnectorConfiguration;
import it.bz.noi.ondemandmerano.model.*;
import it.bz.noi.ondemandmerano.model.iternitystep.OnDemandMeranoActivityHaltPrivate;
import it.bz.noi.ondemandmerano.model.iternitystep.OnDemandMeranoIternityStep;
import it.bz.noi.ondemandmerano.model.iternitystep.OnDemandMeranoRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * encapsulates the communication with the A22 web service
 */
@Component
public class OnDemandMeranoConnector {

    private static final Logger LOG = LoggerFactory.getLogger(OnDemandMeranoConnector.class);

    private static final String STOPS_PATH = "/v2.0/stops";
    private static final String ACTIVE_ACTIVITIES_PATH = "/v1.0/activities/active";
    private static final String POLYGONS_PATH = "/v1.0/polygons";

    @Autowired
    private ConnectorConfiguration connectorConfiguration;

    public List<OnDemandMeranoStop> getStops() throws IOException {

        HttpURLConnection conn = (HttpURLConnection) (new URL(connectorConfiguration.getURL() + STOPS_PATH))
                .openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthorizationHeader());
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("could not get stops (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        conn.disconnect();

        // parse response

        ArrayList<OnDemandMeranoStop> output = new ArrayList<>();

        JsonArray jsonArray;
        try {
            jsonArray = new Gson().fromJson(response.toString(), JsonArray.class);
        } catch (Exception e) {
            // throw an error in case not even the top level element cannot be extracted as
            // expected
            LOG.warn("---");
            LOG.warn("getStops() ERROR: unparsable response:");
            LOG.warn("vvv");
            LOG.warn(response.toString());
            LOG.warn("^^^");
            LOG.warn(e.getMessage(), e);
            LOG.warn("---");
            throw new IOException("ERROR: unparsable response");
        }

        int skipped = 0;
        int i;
        for (i = 0; i < jsonArray.size(); i++) {

            try {
                JsonObject stopJson = jsonArray.get(i).getAsJsonObject();

                OnDemandMeranoStop stop = new OnDemandMeranoStop();
                stop.setId(extractLong(stopJson, "id"));
                stop.setTitle(extractString(stopJson, "title"));
                stop.setReference(extractString(stopJson, "reference"));
                stop.setType(extractString(stopJson, "type"));

                JsonObject geoAddress = extractJsonObject(stopJson, "geoAddress");
                JsonObject position = extractJsonObject(geoAddress, "position");
                JsonObject streetAddress = extractJsonObject(geoAddress, "streetAddress");
                JsonArray groups = extractJsonArray(stopJson, "groups");
                JsonObject region = extractJsonObject(stopJson, "region");

                stop.setPosition(new Gson().fromJson(position, OnDemandServicePositionPoint.class));
                stop.setAddress(new Gson().fromJson(streetAddress, OnDemandMeranoStopAddress.class));
                stop.setGroups(new Gson().fromJson(groups, new TypeToken<ArrayList<OnDemandMeranoGroup>>() {
                }.getType()));
                stop.setRegion(new Gson().fromJson(region, OnDemandMeranoRegion.class));

                output.add(stop);
            } catch (Exception e) {

                // null pointer, cast or number format exception in case the json hasn't the
                // expected form
                // or has incompatible data types: log and skip the record
                LOG.warn("---");
                LOG.warn("getStops() ERROR: skipping unparsable record:");
                LOG.warn("vvv");
                LOG.warn(jsonArray.get(i).toString());
                LOG.warn("^^^");
                LOG.warn(e.getMessage(), e);
                LOG.warn("---");
                skipped++;
                continue;
            }

        }

        LOG.debug("getStops() OK: got data about " + output.size() + " stops, " + skipped + " skipped");

        return output;
    }

    public List<OnDemandMeranoPolygon> getPolygons() throws IOException {

        HttpURLConnection conn = (HttpURLConnection) (new URL(connectorConfiguration.getURL() + POLYGONS_PATH))
                .openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthorizationHeader());
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("could not get polygons (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        conn.disconnect();

        // parse response

        ArrayList<OnDemandMeranoPolygon> output = new ArrayList<>();

        JsonArray jsonArray;
        try {
            jsonArray = new Gson().fromJson(response.toString(), JsonArray.class);
        } catch (Exception e) {
            // throw an error in case not even the top level element cannot be extracted as
            // expected
            LOG.warn("---");
            LOG.warn("getPolygons() ERROR: unparsable response:");
            LOG.warn("vvv");
            LOG.warn(response.toString());
            LOG.warn("^^^");
            LOG.warn(e.getMessage(), e);
            LOG.warn("---");
            throw new IOException("ERROR: unparsable response");
        }

        int skipped = 0;
        int i;
        for (i = 0; i < jsonArray.size(); i++) {

            try {
                JsonObject polygonJson = jsonArray.get(i).getAsJsonObject();

                OnDemandMeranoPolygon polygon = new OnDemandMeranoPolygon();
                polygon.setId(extractInteger(polygonJson, "id"));
                polygon.setName(extractString(polygonJson, "name"));
                polygon.setDescription(extractString(polygonJson, "description"));

                JsonObject borders = extractJsonObject(polygonJson, "borders");
                JsonObject geometry = extractJsonObject(borders, "geometry");
                polygon.setGeometry(new Gson().fromJson(geometry, OnDemandServiceGeometryPolygon.class));

                JsonObject region = extractJsonObject(polygonJson, "region");
                polygon.setRegion(new Gson().fromJson(region, OnDemandMeranoRegion.class));

                output.add(polygon);
            } catch (Exception e) {

                // null pointer, cast or number format exception in case the json hasn't the
                // expected form
                // or has incompatible data types: log and skip the record
                LOG.warn("---");
                LOG.warn("getPolygons() ERROR: skipping unparsable record:");
                LOG.warn("vvv");
                LOG.warn(jsonArray.get(i).toString());
                LOG.warn("^^^");
                LOG.warn(e.getMessage(), e);
                LOG.warn("---");
                skipped++;
                continue;
            }

        }

        LOG.debug("getPolygons() OK: got data about " + output.size() + " polygons, " + skipped + " skipped");

        return output;
    }

    public List<OnDemandMeranoActivity> getActivities() throws IOException {

        HttpURLConnection conn = (HttpURLConnection) (new URL(connectorConfiguration.getURL() + ACTIVE_ACTIVITIES_PATH))
                .openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthorizationHeader());
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("could not get active activities (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        conn.disconnect();

        // parse response

        ArrayList<OnDemandMeranoActivity> output = new ArrayList<>();

        JsonArray jsonArray;
        try {
            jsonArray = new Gson().fromJson(response.toString(), JsonArray.class);
        } catch (Exception e) {
            // throw an error in case not even the top level element cannot be extracted as
            // expected
            LOG.warn("---");
            LOG.warn("getActivities() ERROR: unparsable response:");
            LOG.warn("vvv");
            LOG.warn(response.toString());
            LOG.warn("^^^");
            LOG.warn(e.getMessage(), e);
            LOG.warn("---");
            throw new IOException("ERROR: unparsable response");
        }

        int skipped = 0;
        int i;
        for (i = 0; i < jsonArray.size(); i++) {

            try {
                JsonObject jsonRecord = jsonArray.get(i).getAsJsonObject();

                OnDemandMeranoActivity record = new OnDemandMeranoActivity();
                record.setId(extractLong(jsonRecord, "id"));
                record.setState(extractString(jsonRecord, "state"));
                String startedAt = extractString(jsonRecord, "startedAt");
                if (startedAt != null)
                    record.setStartAt(ZonedDateTime.parse(startedAt));
                record.setPlannedStartAt(ZonedDateTime.parse(extractString(jsonRecord, "plannedStartAt")));
                record.setUpdatedAt(ZonedDateTime.parse(extractString(jsonRecord, "updatedAt")));
                record.setItineraryDone(jsonToItineryList(jsonRecord.get("itineraryDone").getAsJsonArray()));
                record.setItineraryRemaining(jsonToItineryList(jsonRecord.get("itineraryRemaining").getAsJsonArray()));
                record.setVehicle(jsonToVehicle(jsonRecord));

                output.add(record);

            } catch (Exception e) {

                // null pointer, cast or number format exception in case the json hasn't the
                // expected form
                // or has incompatible data types: log and skip the record
                LOG.warn("---");
                LOG.warn("getVehicles() ERROR: skipping unparsable record:");
                LOG.debug("vvv");
                LOG.debug(jsonArray.get(i).toString());
                LOG.debug("^^^");
                LOG.debug(e.getMessage(), e);
                LOG.warn("---");
                skipped++;
                continue;
            }

        }

        LOG.debug("getVehicles() OK: got data about " + output.size() + " activities, " + skipped + " skipped");

        return output;
    }

    private String getBasicAuthorizationHeader() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((connectorConfiguration.getUsername() + ":" + connectorConfiguration.getPassword())
                        .getBytes(StandardCharsets.UTF_8));
    }

    private List<OnDemandMeranoIternityStep> jsonToItineryList(JsonArray itineraryJsonArray) {
        List<OnDemandMeranoIternityStep> iternityStepList = new ArrayList<>();
        for (int i = 0; i < itineraryJsonArray.size(); i++) {
            JsonObject itinerary = itineraryJsonArray.get(i).getAsJsonObject();
            String itineraryType = itinerary.get("type").getAsString();
            if (itineraryType.equals("ACTIVITY_HALT_PRIVATE")) {
                OnDemandMeranoActivityHaltPrivate onDemandMeranoActivityHaltPrivate = new OnDemandMeranoActivityHaltPrivate();
                onDemandMeranoActivityHaltPrivate.setTime(
                        new Gson().fromJson(extractJsonObject(itinerary, "time"), HashMap.class));

                JsonObject stopJsonObject = extractJsonObject(itinerary, "stop");
                OnDemandMeranoActivityHaltPrivate.Stop onDemandMeranoActivityHaltPrivateStop = new OnDemandMeranoActivityHaltPrivate.Stop();
                onDemandMeranoActivityHaltPrivateStop.setId(extractString(stopJsonObject, "id"));
                onDemandMeranoActivityHaltPrivateStop.setType(extractString(stopJsonObject, "type"));
                onDemandMeranoActivityHaltPrivateStop.setTitle(extractString(stopJsonObject, "title"));
                onDemandMeranoActivityHaltPrivateStop.setPosition(
                        new Gson().fromJson(extractJsonObject(stopJsonObject, "position"),
                                OnDemandServicePositionPoint.class));
                onDemandMeranoActivityHaltPrivateStop.setStreetAddress(
                        new Gson().fromJson(extractJsonObject(stopJsonObject, "streetAddress"),
                                OnDemandMeranoStopAddress.class));
                onDemandMeranoActivityHaltPrivate.setStop(onDemandMeranoActivityHaltPrivateStop);

                onDemandMeranoActivityHaltPrivate.setDropOffCapacities(
                        new Gson().fromJson(extractJsonObject(itinerary, "dropOffCapacities"),
                                new TypeToken<HashMap<String, Integer>>() {
                                }.getType()));
                onDemandMeranoActivityHaltPrivate.setPickUpCapacities(
                        new Gson().fromJson(extractJsonObject(itinerary, "pickUpCapacities"),
                                new TypeToken<HashMap<String, Integer>>() {
                                }.getType()));
                iternityStepList.add(onDemandMeranoActivityHaltPrivate);
            } else if (itineraryType.equals("ROUTE")) {
                OnDemandMeranoRoute onDemandMeranoRoute = new OnDemandMeranoRoute();
                onDemandMeranoRoute.setRouteEncoded(itinerary.get("routeEncoded").getAsString());
                iternityStepList.add(onDemandMeranoRoute);
            } else {
                LOG.debug("Invalid itinerary type: {}", itineraryType);
                throw new RuntimeException("Invalid itinerary type: " + itineraryType);
            }
        }
        return iternityStepList;
    }

    private OnDemandMeranoVehicle jsonToVehicle(JsonObject jsonRecord) {

        JsonObject vehicleRecord = extractJsonObject(jsonRecord, "bus");
        JsonObject latestPosition = extractJsonObject(jsonRecord, "latestPosition");

        OnDemandMeranoVehicle vehicle = new OnDemandMeranoVehicle();
        vehicle.setLicensePlateNumber(extractString(vehicleRecord, "licensePlateNumber"));

        JsonObject type = extractJsonObject(vehicleRecord, "type");
        JsonObject operator = extractJsonObject(vehicleRecord, "operator");
        JsonObject capacityMax = extractJsonObject(vehicleRecord, "capacityMax");
        JsonObject capacityUsed = extractJsonObject(vehicleRecord, "capacityUsed");

        vehicle.setType(new Gson().fromJson(type, HashMap.class));
        vehicle.setOperator(new Gson().fromJson(operator, OnDemandMeranoOperator.class));
        vehicle.setCapacityMax(new Gson().fromJson(capacityMax,
                new TypeToken<HashMap<String, Integer>>() {
                }.getType()));
        vehicle.setCapacityUsed(new Gson().fromJson(capacityUsed,
                new TypeToken<HashMap<String, Integer>>() {
                }.getType()));

        vehicle.setRecordTime(extractString(latestPosition, "recordTime"));
        
        if (latestPosition != null) {
            JsonObject position = extractJsonObject(latestPosition, "position");
            vehicle.setPosition(new Gson().fromJson(position, OnDemandServicePositionPoint.class));
        }

        return vehicle;
    }

    public String extractString(JsonObject obj, String key) throws IllegalArgumentException {
        if (obj == null || key == null) {
            return null;
        }
        JsonElement prop = obj.get(key);
        if (prop == null || prop.isJsonNull()) {
            return null;
        }
        if (prop.isJsonPrimitive())
            return prop.getAsString();
        throw new IllegalArgumentException("value cannot be interpreted as String");
    }

    public Long extractLong(JsonObject obj, String key) throws IllegalArgumentException {
        JsonElement prop = obj.get(key);
        if (prop == null || prop.isJsonNull())
            return null;
        if (prop.isJsonPrimitive())
            return prop.getAsLong();
        throw new IllegalArgumentException("value cannot be interpreted as Long");
    }

    public Integer extractInteger(JsonObject obj, String key) throws IllegalArgumentException {
        JsonElement prop = obj.get(key);
        if (prop == null || prop.isJsonNull())
            return null;
        if (prop.isJsonPrimitive())
            return prop.getAsInt();
        throw new IllegalArgumentException("value cannot be interpreted as Long");
    }

    public JsonObject extractJsonObject(JsonObject obj, String key) throws IllegalArgumentException {
        JsonElement prop = obj.get(key);
        if (prop == null || prop.isJsonNull())
            return null;
        if (prop.isJsonObject())
            return prop.getAsJsonObject();
        throw new IllegalArgumentException("value cannot be interpreted as JsonObject");
    }

    public JsonArray extractJsonArray(JsonObject obj, String key) throws IllegalArgumentException {
        JsonElement prop = obj.get(key);
        if (prop == null || prop.isJsonNull())
            return null;
        if (prop.isJsonArray())
            return prop.getAsJsonArray();
        throw new IllegalArgumentException("value cannot be interpreted as JsonArray");
    }
}
