// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

import java.util.ArrayList;

public class OnDemandMeranoStop {

    private Long id;
    private String title;
    private String reference;
    private String type;
    private OnDemandServicePositionPoint position;
    private OnDemandMeranoStopAddress address;
    private ArrayList<OnDemandMeranoGroup> groups;
    private OnDemandMeranoRegion region;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OnDemandServicePositionPoint getPosition() {
        return position;
    }

    public void setPosition(OnDemandServicePositionPoint position) {
        this.position = position;
    }

    public OnDemandMeranoStopAddress getAddress() {
        return address;
    }

    public void setAddress(OnDemandMeranoStopAddress address) {
        this.address = address;
    }

    public ArrayList<OnDemandMeranoGroup> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<OnDemandMeranoGroup> groups) {
        this.groups = groups;
    }

    public OnDemandMeranoRegion getRegion() {
        return region;
    }

    public void setRegion(OnDemandMeranoRegion region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "OnDemandMeranoStop{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", reference='" + reference + '\'' +
                ", type='" + type + '\'' +
                ", position=" + position +
                ", address=" + address +
                ", groups=" + groups +
                ", region=" + region +
                '}';
    }
}
