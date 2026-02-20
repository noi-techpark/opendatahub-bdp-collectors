// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:ondemandmerano.properties")
public class OnDemandMeranoConfiguration {

    @Value( "${origin}" )
    private String origin;

    @Value("${stop.stationtype}")
    private String stopsStationtype;

    @Value("${vehicle.stationtype}")
    private String vehiclesStationtype;

    @Value("${itinerary.stationtype}")
    private String itineraryStationtype;

    @Value("${polygon.category}")
    private String polygonsCategory;

    @Value("${vehicle.period}")
    private Integer vehiclesPeriod;

    @Value("${itinerary.period}")
    private Integer itineraryPeriod;

    @Value("${polygon.uuid-prefix}")
    private String polygonUuidPrefix;

    public String getOrigin() {
        return origin;
    }

    public String getStopsStationtype() {
        return stopsStationtype;
    }

    public String getVehiclesStationtype() {
        return vehiclesStationtype;
    }

    public Integer getVehiclesPeriod() {
        return vehiclesPeriod;
    }

    public Integer getItineraryPeriod() {
        return itineraryPeriod;
    }

    public String getItineraryStationtype() {
        return itineraryStationtype;
    }

    public String getPolygonsCategory() {
        return polygonsCategory;
    }

    public String getPolygonUuidPrefix() {
        return polygonUuidPrefix;
    }
}
