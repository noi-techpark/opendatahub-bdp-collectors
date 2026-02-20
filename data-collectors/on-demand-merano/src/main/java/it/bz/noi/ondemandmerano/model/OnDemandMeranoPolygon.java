// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

public class OnDemandMeranoPolygon {

    private Integer id;
    private String name;
    private String description;
    private OnDemandServiceGeometryPolygon geometry;
    private OnDemandMeranoRegion region;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OnDemandServiceGeometryPolygon getGeometry() {
        return geometry;
    }

    public void setGeometry(OnDemandServiceGeometryPolygon geometry) {
        this.geometry = geometry;
    }

    public OnDemandMeranoRegion getRegion() {
        return region;
    }

    public void setRegion(OnDemandMeranoRegion region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "OnDemandMeranoPolygon{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", geometry=" + geometry +
                ", region=" + region +
                '}';
    }
}
