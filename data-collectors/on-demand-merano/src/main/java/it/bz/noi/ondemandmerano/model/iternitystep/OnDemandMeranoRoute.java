// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model.iternitystep;

import java.util.HashMap;

public class OnDemandMeranoRoute extends OnDemandMeranoIternityStep {

    private String routeEncoded;

    public OnDemandMeranoRoute() {
        this.type = "ROUTE";
    }

    public String getRouteEncoded() {
        return routeEncoded;
    }

    public void setRouteEncoded(String routeEncoded) {
        this.routeEncoded = routeEncoded;
    }

    @Override
    public HashMap<String, Object> toJson() {
        HashMap<String, Object> json = new HashMap<>();
        json.put("type", type);
        json.put("routeEncoded", routeEncoded);
        return json;
    }

    @Override
    public String toString() {
        return "OnDemandMeranoRoute{" +
                "type='" + type + '\'' +
                "routeEncoded='" + routeEncoded + '\'' +
                '}';
    }
}
