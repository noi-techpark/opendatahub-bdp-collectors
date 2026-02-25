// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.odh.trafficprovbz.dto.AggregatedDataDto;
import it.bz.odh.trafficprovbz.dto.LaneDto;
import it.bz.odh.trafficprovbz.dto.MetadataDto;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {

	@Test
	void checkIfCreationOfStationWorks() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();

		String metadata = "[{ \"id\": 1, \"nome\": \"4\",\"geoInfo\": {\"latitudine\": 46.4497009548582, \"longitudine\":11.3448734664564, \"regione\":\"Trentino-Alto Adige\", \"provincia\":\"Bolzano\", \"comune\":\"Laives\"},\"stradaInfo\": {\"nome\": \"SS 12 dell'Abetone e del Brennero\", \"chilometrica\": 432.69},\"direzioni\": [{\"tipo\": \"ascendente\",\"descrizione\": \"Verso Bolzano\"},{\"tipo\": \"discendente\", \"descrizione\": \"Verso Trento\"}], \"schemaDiClassificazione\": 1,\"numeroCorsie\": 2,\"corsieInfo\": [{\"id\": 1,\"descrizione\": \"verso Bolzano\", \"sensoDiMarcia\": \"ascendente\"},{\"id\": 2,\"descrizione\": \"verso Trento\",\"sensoDiMarcia\": \"discendente\"}]}]";
		MetadataDto[] stationsUnderTest = objectMapper.readValue(metadata, MetadataDto[].class);
		JSONObject otherFields = new JSONObject(stationsUnderTest[0].getOtherFields());
		ArrayList<LinkedHashMap<String, String>> lanes = JsonPath.read(otherFields, "$.corsieInfo");

		for (LinkedHashMap<String, String> lane : lanes) {
			StationDto stationUnderTest = Parser.createStation(stationsUnderTest[0], otherFields, lane,
					"TrafficSensor");
			// check for Nome, because Nome is used for id
			assertThat(stationUnderTest.getId()).startsWith("4");
			assertThat(46.4497009548582).isEqualTo(stationUnderTest.getLatitude());
			assertThat(11.3448734664564).isEqualTo(stationUnderTest.getLongitude());
		}
	}

	@Test
	void checkIfCreationOfTrafficMeasurementWorks() throws JsonProcessingException, ParseException {
		ObjectMapper objectMapper = new ObjectMapper();
		Integer period = 300;

		String aggregatedData = "[{\"idPostazione\": 3, \"data\": \"2021-12-02T11:10:00Z\", \"corsia\": 0, \"direzione\": \"ascendente\", \"totaleVeicoli\": 64, \"totaliPerClasseVeicolare\": { \"2\": 59, \"4\": 5 }, \"mediaArmonicaVelocita\": 79.3, \"headwayMedioSecondi\": 4.68, \"varianzaHeadwayMedioSecondi\": 26.01, \"gapMedioSecondi\": 4.42, \"varianzaGapMedioSecondi\": 26.12}]";
		AggregatedDataDto[] aggregatedDataDtos = objectMapper.readValue(aggregatedData, AggregatedDataDto[].class);

		DataMapDto<RecordDtoImpl> rootMap = new DataMapDto<>();

		DataMapDto<RecordDtoImpl> stationMap = rootMap.upsertBranch(aggregatedDataDtos[0].getId());
		Parser.insertDataIntoStationMap(aggregatedDataDtos, period, stationMap, new LaneDto("0", "ascendente"));

	}
}
