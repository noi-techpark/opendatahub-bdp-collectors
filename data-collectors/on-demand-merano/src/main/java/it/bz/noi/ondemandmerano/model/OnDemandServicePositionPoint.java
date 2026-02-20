// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class OnDemandServicePositionPoint {

    private String type;
    private List<Double> coordinates;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Double> coordinates) {
        this.coordinates = coordinates;
    }

    @JsonIgnore
    public Double getLongitude() {
        if(coordinates != null && coordinates.size() >= 2)
            return coordinates.get(0);
        return null;
    }

    @JsonIgnore
    public Double getLatitude() {
        if(coordinates != null && coordinates.size() >= 2)
            return coordinates.get(1);
        return null;
    }

    @Override
    public String toString() {
        return "OnDemandServicePositionPoint{" +
                "type='" + type + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
