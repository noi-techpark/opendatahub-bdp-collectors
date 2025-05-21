// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.opendatahub.bdp.flightdata_realtime;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(DataFetcher.class);
    
    public List<Map<String, String>> getData(String urlString, String bearerToken, String dataset) throws Exception {
        StringBuilder response = new StringBuilder();

        HttpURLConnection connection = (HttpURLConnection) new URI(urlString + "?params[" + URLEncoder.encode(dataset, "UTF-8") + "]=LIPB").toURL().openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        
        return mapJsonData(response.toString());
    }

    public List<Map<String, String>> mapJsonData(String jsonData) {

        List<Map<String, String>> correctionsList = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);

            for (JsonNode node : rootNode) {

                Map<String, String> externalStation = new HashMap<>();

                List<String> keys = Arrays.asList(
                    "ID", 
                    "FlightLogID", 
                    "DEP", 
                    "DEST", 
                    "STD", 
                    "STA",
                    "ACFTAIL",
                    "ATCEET",
                    "ReleasedForDispatch",
                    "TOA",
                    "ATCID",
                    "LatestFlightPlanDate",
                    "Alt1",
                    "Alt2",
                    "ExternalFlightID",
                    "GUFI",
                    "IsRecalc",
                    "DEPIATA",
                    "DESTIATA",
                    "LastEditDate"
                );

                for (String key : keys) {
                    if(node.has(key)) 
                    {
                        externalStation.put(key, node.get(key).asText());
                    }
                }

                if (node.has("LocalTime")) {
                    ObjectNode localTimeNode = (ObjectNode) node.get("LocalTime");
                    ObjectNode localDeparture = (ObjectNode) localTimeNode.get("Departure");
                    ObjectNode localDestination = (ObjectNode) localTimeNode.get("Destination");

                    externalStation.put("etd-local", localDeparture.get("ETD").asText());
                    externalStation.put("eta-local", localDestination.get("ETA").asText());

                    correctionsList.add(externalStation);
                } 
            }

    
        } catch (IOException e) {
            LOG.error("Cannot extract data from result set.");
        }

        return correctionsList;
    }

}
