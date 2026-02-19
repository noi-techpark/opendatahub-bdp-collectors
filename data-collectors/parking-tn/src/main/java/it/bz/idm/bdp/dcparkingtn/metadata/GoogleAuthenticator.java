// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcparkingtn.metadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;

@Lazy
@Service
public abstract class GoogleAuthenticator {

    @Value("${google.auth.secret_location}")
    private Resource clientSecret;
    private NetHttpTransport HTTP_TRANSPORT;
    private JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    @Value("${google.auth.credentials_folder}")
    private Resource CREDENTIALS_FOLDER;

    protected Credential getCredentials() throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(clientSecret.getInputStream()));
        Set<String> scopes = new HashSet<String>();
        Collections.addAll(scopes, SheetsScopes.SPREADSHEETS,SheetsScopes.DRIVE);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(CREDENTIALS_FOLDER.getFile()))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    @PostConstruct
    private void init() throws GeneralSecurityException, IOException {
		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		this.initGoogleClient(HTTP_TRANSPORT,JSON_FACTORY,getCredentials());
    }

    public abstract void initGoogleClient(NetHttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, Credential credential)throws IOException;
}
