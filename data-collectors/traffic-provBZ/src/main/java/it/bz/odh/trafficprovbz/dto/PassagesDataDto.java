// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class PassagesDataDto {

	@JsonProperty("idPostazione")
	private int id;

	@JsonProperty("data")
	private String date;

	@JsonProperty("idVeicolo")
	private String idVehicle;

	private final Map<String, Object> passages = new HashMap<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getIdVehicle() {
		return idVehicle;
	}

	public void setIdVehicle(String idVehicle) {
		this.idVehicle = idVehicle;
	}

	// Capture all other fields that Jackson do not match
	@JsonAnyGetter
	public Map<String, Object> getPassages() {
		return passages;
	}

	@JsonAnySetter
	public void setPassage(String name, Object value) {
		passages.put(name, value);
	}
}
