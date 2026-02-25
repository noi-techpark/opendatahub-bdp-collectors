// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class MetadataDto {

	@JsonProperty("id")
	private String id;

	@JsonProperty("nome")
	private String name;

	@JsonProperty("schemiDiClassificazione")
	private int classificationSchema;

	private final Map<String, Object> otherFields = new HashMap<>();

	// save odhId because name/id mismatch and to save the data the odhId is needed
	// also direction is needed becuase FAMAS API gives data for one lane in both
	// directions, but only the correct one is needed
	private HashMap<String, LaneDto> lanes;

	public Map<String, LaneDto> getLanes() {
		return lanes;
	}

	public void addLane(String stationId, String laneId, String direction) {
		if (lanes == null)
			lanes = new HashMap<>();
		lanes.put(stationId, new LaneDto(laneId, direction));
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getClassificationSchema() {
		return this.classificationSchema;
	}

	public void setClassificationSchema(int classificationSchema) {
		this.classificationSchema = classificationSchema;
	}

	// Capture ac
	@JsonAnyGetter
	public Map<String, Object> getOtherFields() {
		return otherFields;
	}

	@JsonAnySetter
	public void setOtherField(String name, Object value) {
		otherFields.put(name, value);
	}
}
