// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class AggregatedDataDto {

	@JsonProperty("idPostazione")
	private String id;

	@JsonProperty("data")
	private String date;

	@JsonProperty("corsia")
	private String lane;

	@JsonProperty("direzione")
	private String direction;

	@JsonProperty("totaleVeicoli")
	private Double totalTransits;

	@JsonProperty("mediaArmonicaVelocita")
	private Double averageVehicleSpeed;

	@JsonProperty("headwayMedioSecondi")
	private Double headway;

	@JsonProperty("varianzaHeadwayMedioSecondi")
	private Double headwayVariance;

	@JsonProperty("gapMedioSecondi")
	private Double gap;

	@JsonProperty("varianzaGapMedioSecondi")
	private Double gapVariance;

	private final Map<String, Object> otherFields = new HashMap<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getLane() {
		return lane;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public Double getTotalTransits() {
		return totalTransits;
	}

	public void setTotalTransits(Double totalTransits) {
		this.totalTransits = totalTransits;
	}

	public Double getAverageVehicleSpeed() {
		return averageVehicleSpeed;
	}

	public void setAverageVehicleSpeed(Double averageVehicleSpeed) {
		this.averageVehicleSpeed = averageVehicleSpeed;
	}

	public Double getHeadway() {
		return headway;
	}

	public void setHeadway(Double headway) {
		this.headway = headway;
	}

	public Double getHeadwayVariance() {
		return headwayVariance;
	}

	public void setHeadwayVariance(Double headwayVariance) {
		this.headwayVariance = headwayVariance;
	}

	public Double getGap() {
		return gap;
	}

	public void setGap(Double gap) {
		this.gap = gap;
	}

	public Double getGapVariance() {
		return gapVariance;
	}

	public void setGapVariance(Double gapVariance) {
		this.gapVariance = gapVariance;
	}

	// Capture all other fields that Jackson do not match
	@JsonAnyGetter
	public Map<String, Object> getOtherFields() {
		return otherFields;
	}

	@JsonAnySetter
	public void setOtherField(String name, Object value) {
		otherFields.put(name, value);
	}
}
