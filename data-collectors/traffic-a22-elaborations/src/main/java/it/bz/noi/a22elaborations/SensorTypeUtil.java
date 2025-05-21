// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22elaborations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;

@Component
public class SensorTypeUtil {

    private Logger logger = LoggerFactory.getLogger(SensorTypeUtil.class);

    private Map<String, String> sensorTypeByStation;

    public static boolean isCamera(StationDto station) {
        return "camera".equals(station.getMetaData().get("sensor_type"));
    }

    public void addSensorTypeMetadata(StationList stations) {
        if (sensorTypeByStation == null) {
            initializeMap();
        }
        for (StationDto station : stations) {
            String sensorType = sensorTypeByStation.getOrDefault(station.getId(), null);
            if (sensorType != null && !sensorType.isEmpty()) {
                station.getMetaData().put("sensor_type", sensorType.trim());
            } else {
                logger.info("Station with code {} not found in sensor-type-mapping.csv", station.getId());
            }
        }

    }

    private void initializeMap() {
        // read sensor type <--> station code mapping from csv
        sensorTypeByStation = new HashMap<>();

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classloader.getResourceAsStream("sensor-type-mapping.csv");
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        BufferedReader br = new BufferedReader(streamReader);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String code = values[0];
                String sensorType = values[1];
                sensorTypeByStation.put(code, sensorType);
            }
        } catch (IOException e) {
            logger.error("Error while reading sensor-type-mapping.csv");
        }
    }
}
