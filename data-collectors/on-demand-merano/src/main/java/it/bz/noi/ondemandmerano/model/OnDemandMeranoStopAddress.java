// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

public class OnDemandMeranoStopAddress {

    private String state;
    private String city;
    private String postalcode;
    private String housenumber;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    public String getHousenumber() {
        return housenumber;
    }

    public void setHousenumber(String housenumber) {
        this.housenumber = housenumber;
    }

    @Override
    public String toString() {
        return "OnDemandMeranoStopAddress{" +
                "state='" + state + '\'' +
                ", city='" + city + '\'' +
                ", postalcode='" + postalcode + '\'' +
                ", housenumber='" + housenumber + '\'' +
                '}';
    }
}
