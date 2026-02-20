// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.pusher;

import org.springframework.stereotype.Component;

@Component
public class VehicleJSONPusher extends OnDemandServiceJSONPusher {

    @Override
    public String initIntegreenTypology() {
        return onDemandMeranoConfiguration.getVehiclesStationtype();
    }
}
