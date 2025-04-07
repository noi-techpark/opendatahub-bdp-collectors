// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22elaborations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EUROTypeUtil {
    static final String EURO0 = "EURO0";
    static final String EURO1 = "EURO1";
	static final String EURO2 = "EURO2";
	static final String EURO3 = "EURO3";
	static final String EURO4 = "EURO4";
	static final String EURO5 = "EURO5";
	static final String EURO6 = "EURO6";
	static final String EUROE = "ELECTRIC";

    private Logger logger = LoggerFactory.getLogger(EUROTypeUtil.class);

    private Map<String, EUROType> vehicleDataMap2023;
    private Map<String, EUROType> vehicleDataMap2024;
    private Timestamp cutoff2024 = Timestamp.valueOf("2024-01-01 00:00:00");

    public Map<String, EUROType> getVehicleDataMap(Timestamp timestamp) {
        if (vehicleDataMap2023 == null || vehicleDataMap2024 == null) {
            initializeMap();
        }

        if (timestamp.before(cutoff2024)) {
            return vehicleDataMap2023;
        }

        return vehicleDataMap2024;
    }

    private void initializeMap() {
        vehicleDataMap2023 = new HashMap<>();
        vehicleDataMap2024 = new HashMap<>();

        readCSV("associaz_prob_euro_targa_AV_2023.csv", vehicleDataMap2023);
        readCSV("associaz_prob_euro_targa_AV_2024.csv", vehicleDataMap2024);
    }
    
    private void readCSV(String filepath, Map<String, EUROType> map) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classloader.getResourceAsStream(filepath);

        if (inputStream == null) {
            logger.error(String.format("Error: %s not found", filepath));
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = br.readLine(); // Read and ignore header
            if (headerLine == null) {
                logger.error("Error: CSV file is empty");
                return;
            }

            // map headers to EUROTypeUtil excluding first one (targa) which is used as key
            String[] categories = {
                "",
                EUROTypeUtil.EURO0,
                EUROTypeUtil.EURO1,
                EUROTypeUtil.EURO2,
                EUROTypeUtil.EURO3,
                EUROTypeUtil.EURO4,
                EUROTypeUtil.EURO5,
                EUROTypeUtil.EURO6,
                EUROTypeUtil.EUROE,
            };

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                if (values.length != categories.length) {
                    logger.warn("Skipping malformed line: {}", line);
                    continue;
                }

                String targa = values[0]; // First column is the key
                Map<String, Double> probabilities = new HashMap<>();

                for (int i = 1; i < values.length; i++) {
                    try {
                        probabilities.put(categories[i], Double.parseDouble(values[i]));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid number format for Targa {} in column {}: {}", targa, categories[i], values[i]);
                    }
                }

                map.put(targa, new EUROType(targa, probabilities));
            }
        } catch (IOException e) {
            logger.error(String.format("Error while reading %s", filepath), e);
        }
    }
}