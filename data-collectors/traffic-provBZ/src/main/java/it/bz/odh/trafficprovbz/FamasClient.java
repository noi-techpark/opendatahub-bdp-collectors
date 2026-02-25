// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bz.odh.trafficprovbz.dto.AggregatedDataDto;
import it.bz.odh.trafficprovbz.dto.ClassificationSchemaDto;
import it.bz.odh.trafficprovbz.dto.MetadataDto;
import it.bz.odh.trafficprovbz.dto.PassagesDataDto;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;

import javax.annotation.PostConstruct;

@Lazy
@Service
public class FamasClient {
	private static final Logger LOG = LoggerFactory.getLogger(FamasClient.class);

	private static final String RESPONSE_CHARSET = "UTF-8";

	private static final String STATION_ID_URL_PARAM = "%STATION_ID%";

	@Value("${endpoint.classificationSchemas.url}")
	private String classificationSchemasUrl;

	@Value("${endpoint.stationsData.url}")
	private String stationsDataUrl;

	@Value("${endpoint.aggregatedDataOnStations.url}")
	private String aggregatedDataOnStationsUrl;

	@Value("${endpoint.passageDataOnStations.url}")
	private String passagesDataOnStationsUrl;

	@Value("${endpoint.user}")
	private String user;

	@Value("${endpoint.password}")
	private String password;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private HttpClient client;

	@PostConstruct
	private void initClient() {
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
		provider.setCredentials(AuthScope.ANY, credentials);

		client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		LOG.info("Client initialized");
	}

	/**
	 * This function gets all the classification schemas via an api from famas
	 *
	 * @return ClassificationSchemaDto-array with all the classification schemas
	 * @throws IOException used in the code to throw a failure in input and output
	 *                     operations
	 */
	public ClassificationSchemaDto[] getClassificationSchemas() throws IOException {
		HttpResponse response = client.execute(new HttpGet(classificationSchemasUrl));
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		LOG.debug("Dumping json for getClassificationSchemas: " +responseString);
		return objectMapper.readValue(responseString, ClassificationSchemaDto[].class);
	}

	/**
	 * This function gets all the data about the stations via an api from famas
	 *
	 * @return MetadataDto-array with all the stations
	 * @throws IOException used in the code to throw a failure in input and output
	 *                     operations
	 */
	public MetadataDto[] getStationsData() throws IOException {
		HttpResponse response = client.execute(new HttpGet(stationsDataUrl));
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		LOG.debug("Dumping json for getStationsDate: " +responseString);
		return objectMapper.readValue(responseString, MetadataDto[].class);
	}

	/**
	 * This function gets all the traffic data about the stations via an api from
	 * famas
	 *
	 * @return AggregatedDataDto-array with all traffic of the stations
	 * @throws IOException used in the code to throw a failure in input and output
	 *                     operations
	 */
	public AggregatedDataDto[] getAggregatedDataOnStations(String stationId, String startPeriod, String endPeriod)
			throws IOException {
		JSONObject payload = new JSONObject();
		JSONArray stationIdArray = new JSONArray();
		stationIdArray.add(stationId);
		payload.put("IdPostazioni", stationIdArray);
		payload.put("InizioPeriodo", startPeriod);
		payload.put("FinePeriodo", endPeriod);
		String payloadString = payload.toJSONString();
		StringEntity stringEntity = new StringEntity(payloadString,
				ContentType.APPLICATION_JSON);
		HttpPost request = new HttpPost(aggregatedDataOnStationsUrl);
		request.setEntity(stringEntity);
		try {
			HttpEntity entity = client.execute(request).getEntity();
			String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
			LOG.debug("Dumping json for getAggregatedDataOnStations: " +responseString);
			return objectMapper.readValue(responseString, AggregatedDataDto[].class);
		} catch (Exception e) {
			LOG.error("Dumping request object: " + payloadString);
			throw e;
		}
	}

	/**
	 * This function gets all the bluetooth addressess about the devices passed the
	 * stations via an api from famas
	 *
	 * @return PassagesDataDto-array with all bluetooth devices passed the stations
	 * @throws IOException used in the code to throw a failure in input and output
	 *                     operations
	 */
	public PassagesDataDto[] getPassagesDataOnStations(String stationId, String startPeriod, String endPeriod)
			throws IOException {
		JSONObject payload = new JSONObject();
		JSONArray stationIdArray = new JSONArray();
		stationIdArray.add(stationId);
		payload.put("IdPostazioni", stationIdArray);
		payload.put("InizioPeriodo", startPeriod);
		payload.put("FinePeriodo", endPeriod);
		StringEntity stringEntity = new StringEntity(String.valueOf(payload),
				ContentType.APPLICATION_JSON);
		HttpPost request = new HttpPost(passagesDataOnStationsUrl);
		request.setEntity(stringEntity);
		HttpEntity entity = client.execute(request).getEntity();
		String responseString = EntityUtils.toString(entity, RESPONSE_CHARSET);
		LOG.debug("Dumping json for getPassagesDataOnStations: " +responseString);
		return objectMapper.readValue(responseString, PassagesDataDto[].class);
	}
}
