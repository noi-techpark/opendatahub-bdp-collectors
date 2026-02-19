// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.bikechargers.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@PropertySource("classpath:datatypes.properties")
public class DatatypesConfiguration {

    @Value("#{${state}}")
    private Map<String, String> stateMap;

    @Value("#{${freebay}}")
    private Map<String, String> freebayMap;

    @Value("#{${availableVehicles}}")
    private Map<String, String> availableVehiclesMap;

    @Value("#{${usageState}}")
    private Map<String, String> usageStateMap;

    private DatatypeConfiguration state;
    private DatatypeConfiguration freebay;
    private DatatypeConfiguration availableVehicles;
    private DatatypeConfiguration usageState;

    @PostConstruct
    private void init() {
        ObjectMapper mapper = new ObjectMapper();
        state = mapper.convertValue(stateMap, DatatypeConfiguration.class);
        freebay = mapper.convertValue(freebayMap, DatatypeConfiguration.class);
        availableVehicles = mapper.convertValue(availableVehiclesMap, DatatypeConfiguration.class);
        usageState = mapper.convertValue(usageStateMap, DatatypeConfiguration.class);
    }

    public DatatypeConfiguration getState() {
        return state;
    }

    public DatatypeConfiguration getFreebay() {
        return freebay;
    }

    public DatatypeConfiguration getAvailableVehicles() {
        return availableVehicles;
    }

    public DatatypeConfiguration getUsageState() {
        return usageState;
    }

    public List<DatatypeConfiguration> getAllBikeChargerDataTypes() {
        return Arrays.asList(state, freebay, availableVehicles);
    }

    public List<DatatypeConfiguration> getAllBikeChargerBayDataTypes() {
        return Arrays.asList(state, usageState);
    }
}
