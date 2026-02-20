// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

import it.bz.noi.ondemandmerano.model.iternitystep.OnDemandMeranoIternityStep;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OnDemandMeranoActivity {

    private Long id;
    private String state;
    private ZonedDateTime plannedStartAt;
    private ZonedDateTime startAt;
    private ZonedDateTime updatedAt;
    private OnDemandMeranoVehicle vehicle;
    private List<OnDemandMeranoIternityStep> itineraryDone;
    private List<OnDemandMeranoIternityStep> itineraryRemaining;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ZonedDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(ZonedDateTime plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public ZonedDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(ZonedDateTime startAt) {
        this.startAt = startAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OnDemandMeranoVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(OnDemandMeranoVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public List<OnDemandMeranoIternityStep> getItineraryDone() {
        return itineraryDone;
    }

    public void setItineraryDone(List<OnDemandMeranoIternityStep> itineraryDone) {
        this.itineraryDone = itineraryDone;
    }

    public List<OnDemandMeranoIternityStep> getItineraryRemaining() {
        return itineraryRemaining;
    }

    public void setItineraryRemaining(List<OnDemandMeranoIternityStep> itineraryRemaining) {
        this.itineraryRemaining = itineraryRemaining;
    }

    public HashMap<String, Object> toJson() {
        HashMap<String, Object> json = new HashMap<String, Object>();
        json.put("state", state);
        json.put("plannedStartAt", plannedStartAt.toLocalDateTime().toString());
        json.put("startedAt", startAt.toLocalDateTime().toString());
        json.put("itineraryDone", itineraryDone.stream().map(OnDemandMeranoIternityStep::toJson).collect(Collectors.toList()));
        json.put("itineraryRemaining", itineraryRemaining.stream().map(OnDemandMeranoIternityStep::toJson).collect(Collectors.toList()));
        return json;
    }
}
