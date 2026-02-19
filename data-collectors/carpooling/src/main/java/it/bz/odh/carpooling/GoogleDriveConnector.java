// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.carpooling;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class GoogleDriveConnector {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleDriveConnector.class);

	@Value("${googleApi.applicationName}")
	private String applicationName;
	@Value("${googleApi.driveFileId}")
	private String driveFileId;
	@Value("${googleApi.credentialsFile}")
	private String credentialsFilePath;

	private HttpRequestInitializer requestInitializer = null;

	@PostConstruct
	public void init() {
		LOG.info("GoogleDriveConnector init");
		try {
			ServiceAccountCredentials sourceCredentials = ServiceAccountCredentials
				.fromStream(GoogleDriveConnector.class.getResourceAsStream(credentialsFilePath));

			GoogleCredentials googleCredentials = sourceCredentials.createScoped(DriveScopes.DRIVE_READONLY);

			requestInitializer = new HttpCredentialsAdapter(googleCredentials);
		} catch (Exception e) {
			LOG.error("Failed to connect to google API", e);
			throw new RuntimeException(e);
		}
	}

	public InputStream readRidesCsvContent() {
		LOG.info("GoogleDriveConnector readRidesCsvContent");
		try {
			Drive drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), new GsonFactory(),
				requestInitializer)
				.setApplicationName(applicationName).build();


			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			drive.files().get(driveFileId).executeMediaAndDownloadTo(outputStream);

			return new ByteArrayInputStream(outputStream.toByteArray());
		} catch (Exception e) {
			LOG.error("Failed to read rides cs file from google drive", e);
			throw new RuntimeException(e);
		}

	}

}
