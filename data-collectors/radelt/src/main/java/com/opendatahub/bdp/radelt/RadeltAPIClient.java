// SPDX-FileCopyrightText: 2024 NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.opendatahub.bdp.radelt;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendatahub.bdp.radelt.dto.aktionen.AktionenResponseDto;
import com.opendatahub.bdp.radelt.dto.organisationen.OrganisationenResponseDto;

@Component
public class RadeltAPIClient {

    private final Logger LOG = LoggerFactory.getLogger(RadeltAPIClient.class);
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public enum Language {
        ITA, GER
    }

    private final String BASE_URL_ITA = "https://www.altoadigepedala.bz.it";
    private final String BASE_URL_GER = "https://www.suedtirolradelt.bz.it";
    private final String PATH_CHALLENGES = "/dashboard/api/opendata/challenges";
    private final String PATH_ORGANIZATIONS = "/dashboard/api/opendata/organisations";
    

    @Bean
    public static CloseableHttpClient httpClient(){
        int timeout = 30 * 1000;
        RequestConfig rc =  RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();
        return HttpClientBuilder.create().setDefaultRequestConfig(rc).build();
    }
    
    @Autowired
    CloseableHttpClient httpClient;

    public AktionenResponseDto fetchChallenges(String active, String type, Language lang)
            throws Exception {
        String url = lang == Language.GER ? BASE_URL_GER : BASE_URL_ITA;
        url += PATH_CHALLENGES;

        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.setParameter("active", active);
        // uriBuilder.setParameter("limit", limit);
        // uriBuilder.setParameter("offset", offset);
        uriBuilder.setParameter("type", type);

        URI uri = uriBuilder.build();
        HttpGet request = new HttpGet(uri);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    LOG.debug(result);
                    return mapper.readValue(result, AktionenResponseDto.class);
                }
            } else {
                LOG.error("HTTP Request failed with status code: {}", response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching actions", e);
        }
        return null;
    }

    public OrganisationenResponseDto fetchOrganizations(String challengeId, String type,
            String query, Language lang) throws Exception {
        String url = lang == Language.GER ? BASE_URL_GER : BASE_URL_ITA;
        url += PATH_ORGANIZATIONS;

        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.setParameter("challengeId", String.valueOf(challengeId));
        uriBuilder.setParameter("type", type);
        uriBuilder.setParameter("query", query);
        // uriBuilder.setParameter("limit", String.valueOf(limit));
        // uriBuilder.setParameter("offset", String.valueOf(offset));

        URI uri = uriBuilder.build();
        HttpGet request = new HttpGet(uri);
        LOG.info(uri.toString());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    return mapper.readValue(result, OrganisationenResponseDto.class);
                }
            } else {
                LOG.error("HTTP Request failed with status code: {}", response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching organizations", e);
        }
        return null;
    }
}
