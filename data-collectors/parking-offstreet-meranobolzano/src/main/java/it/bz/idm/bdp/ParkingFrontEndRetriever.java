// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ParkingFrontEndRetriever {

	@Value("${parking_url_stations}")
	private String parking_url_stations;

	private static ObjectMapper mapper = new ObjectMapper();

	public String[] getActiveStationIdentifers() {
		URL url;
		try {
			String[] values = null;
			url = new URL(parking_url_stations);
			URLConnection connection = url.openConnection();
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			if (httpConnection.getResponseCode() != 403)
				values = mapper.readValue(httpConnection.getInputStream(), String[].class);
			return values;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
