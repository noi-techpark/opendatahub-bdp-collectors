// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ClassificationSchemaDto {


	@JsonProperty("id")
	private int id;

	@JsonProperty("nome")
	private String name;

	private final Map<String, Object> otherFields = new HashMap<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
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

