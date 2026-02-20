// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

import java.util.List;

public class OnDemandServiceGeometryPolygon {

    private String type;
    private List<List<List<Double>>> coordinates;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<List<List<Double>>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<List<Double>>> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        return "OnDemandServicePositionPoint{" +
                "type='" + type + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
