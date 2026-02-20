// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model.iternitystep;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bz.noi.ondemandmerano.model.OnDemandMeranoStopAddress;
import it.bz.noi.ondemandmerano.model.OnDemandServicePositionPoint;

import java.util.HashMap;
import java.util.Map;

public class OnDemandMeranoActivityHaltPrivate extends OnDemandMeranoIternityStep {

    private HashMap<String, Object> time;
    private OnDemandMeranoActivityHaltPrivate.Stop stop;
    private HashMap<String, Object> dropOffCapacities;
    private HashMap<String, Object> pickUpCapacities;

    public OnDemandMeranoActivityHaltPrivate() {
        this.type = "ACTIVITY_HALT_PRIVATE";
    }

    public HashMap<String, Object> getTime() {
        return time;
    }

    public void setTime(HashMap<String, Object> time) {
        this.time = time;
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public HashMap<String, Object> getDropOffCapacities() {
        return dropOffCapacities;
    }

    public void setDropOffCapacities(HashMap<String, Object> dropOffCapacities) {
        this.dropOffCapacities = dropOffCapacities;
    }

    public HashMap<String, Object> getPickUpCapacities() {
        return pickUpCapacities;
    }

    public void setPickUpCapacities(HashMap<String, Object> pickUpCapacities) {
        this.pickUpCapacities = pickUpCapacities;
    }

    @Override
    public HashMap<String, Object> toJson() {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, Object> json = new HashMap<>();
        json.put("type", type);
        json.put("time", time);
        json.put("stop", mapper.convertValue(stop, new TypeReference<Map<String, Object>>() {}));
        json.put("dropOffCapacities", dropOffCapacities);
        json.put("pickUpCapacities", pickUpCapacities);
        return json;
    }

    @Override
    public String toString() {
        return "OnDemandMeranoActivityHaltPrivate{" +
                "time=" + time +
                ", stop=" + stop +
                ", dropOffCapacities=" + dropOffCapacities +
                ", pickUpCapacities=" + pickUpCapacities +
                ", type='" + type + '\'' +
                '}';
    }

    public static class Stop {
        private String id;
        private String type;
        private String title;
        private OnDemandServicePositionPoint position;
        private OnDemandMeranoStopAddress streetAddress;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public OnDemandServicePositionPoint getPosition() {
            return position;
        }

        public void setPosition(OnDemandServicePositionPoint position) {
            this.position = position;
        }

        public OnDemandMeranoStopAddress getStreetAddress() {
            return streetAddress;
        }

        public void setStreetAddress(OnDemandMeranoStopAddress streetAddress) {
            this.streetAddress = streetAddress;
        }

        @Override
        public String toString() {
            return "Stop{" +
                    "id='" + id + '\'' +
                    ", type='" + type + '\'' +
                    ", title='" + title + '\'' +
                    ", position=" + position +
                    ", streetAddress=" + streetAddress +
                    '}';
        }
    }
}
