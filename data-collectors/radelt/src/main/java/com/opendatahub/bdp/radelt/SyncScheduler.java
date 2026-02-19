// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.opendatahub.bdp.radelt;

import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.opendatahub.bdp.radelt.RadeltAPIClient.Language;
import com.opendatahub.bdp.radelt.dto.aktionen.AktionenResponseDto;
import com.opendatahub.bdp.radelt.dto.aktionen.RadeltChallengeDto;
import com.opendatahub.bdp.radelt.dto.organisationen.OrganisationenResponseDto;
import com.opendatahub.bdp.radelt.utils.MappingUtilsAktionen;
import com.opendatahub.bdp.radelt.utils.MappingUtilsOrganisationen;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationList;

import com.opendatahub.bdp.radelt.utils.DataTypeUtils;
import com.opendatahub.bdp.radelt.utils.CsvImporter;
import com.opendatahub.bdp.radelt.dto.common.RadeltGeoDto;
import java.util.Map;

@Service
public class SyncScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SyncScheduler.class);

	@Autowired
	private OdhClientOrganisations odhClientOrganisations;

	@Autowired
	private OdhClientChallenges odhClientChallenges;

	@Autowired
	private RadeltAPIClient radeltClient;

	@Autowired
	private MappingUtilsOrganisationen mappingUtilsOrganisationen;

	@Autowired
	private MappingUtilsAktionen mappingUtilsAktionen;

	private Map<String, RadeltGeoDto> actionCoordinates;
	private Map<String, RadeltGeoDto> organizationCoordinates;

	@PostConstruct
	private void postConstruct() {
		DataTypeUtils.setupDataType(odhClientChallenges);
		this.actionCoordinates = CsvImporter.syncCsvActions();
		this.organizationCoordinates = CsvImporter.syncCsvOrganizations();
	}

	/**
	 * Scheduled job Aktionen
	 */
	@Scheduled(cron = "${scheduler.cron}")
	public void syncJob() {
		LOG.info("Cron job syncJobAktionen started: Sync Stations with type {} and data types",
				odhClientChallenges.getIntegreenTypology());

		StationList stationsOrganisationen = new StationList();
		DataMapDto<RecordDtoImpl> dataOrganisationen = new DataMapDto<>();

		StationList stationsChallenges = new StationList();
		DataMapDto<RecordDtoImpl> dataChallenges = new DataMapDto<>();

		AktionenResponseDto challengeResponseDtoGer;
		AktionenResponseDto challengeResponseDtoIta;

		try {
			challengeResponseDtoGer = radeltClient.fetchChallenges("true", "DISTANCE", Language.GER);
			challengeResponseDtoIta = radeltClient.fetchChallenges("true", "DISTANCE", Language.ITA);

			mappingUtilsAktionen.mapData(challengeResponseDtoGer, challengeResponseDtoIta, this.actionCoordinates,
					stationsChallenges,
					dataChallenges);

			for (RadeltChallengeDto challengeDto : challengeResponseDtoGer.getData().getChallenges()) {
				// Define base URL for organizations
				// Initialize pagination variables
				// Fetch and process organizations
				OrganisationenResponseDto organizationResponseDtoGer;

				try {
					organizationResponseDtoGer = radeltClient.fetchOrganizations(
							String.valueOf(challengeDto.getId()),
							"", "", Language.GER);
					mappingUtilsOrganisationen.mapData(organizationResponseDtoGer,
							this.organizationCoordinates, stationsOrganisationen, dataOrganisationen);

				} catch (Exception e) {
					e.printStackTrace();
					LOG.error("Error fetching organizations with challenge id {}. Error message: {}",
							challengeDto.getId(),
							e.getMessage());
					return; // Exit the method if fetching organizations fails
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Error fetching challenges: {}", e.getMessage());
		}

		try {
			// Sync with Open Data Hub
			odhClientOrganisations.syncStations(stationsOrganisationen);
			odhClientChallenges.syncStations(stationsChallenges);
			LOG.info("Syncing stations successful");
			odhClientOrganisations.pushData(dataOrganisationen);
			odhClientChallenges.pushData(dataChallenges);
			LOG.info("Pushing data successful");
		} catch (WebClientRequestException e) {
			LOG.error("Syncing stations failed: Request exception: {}", e.getMessage());
		}

		LOG.info("Cron job completed successfully");
	}
}
