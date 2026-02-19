// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcbikesharingbz;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dcbikesharingbz.dto.BikesharingBzDto;
import it.bz.idm.bdp.dcbikesharingbz.dto.BikesharingBzStationDto;

@Component
@PropertySource({ "classpath:/META-INF/spring/application.properties" })
public class BikesharingBzDataRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(BikesharingBzDataRetriever.class.getName());

    @Autowired
    private Environment env;

    @Autowired
    private BikesharingBzDataConverter converter;

    private CloseableHttpClient clientStations;
    private CloseableHttpClient clientMeasurements;

    private String endpointMethodStations;
    private String serviceUrlStations;

    private String endpointMethodMeasurements;
    private String serviceUrlMeasurements;

    public BikesharingBzDataRetriever() {
        LOG.debug("Create instance");
    }

    @PostConstruct
    private void initClient() {
        LOG.debug("Init");
        if ( clientStations == null ) {
            //Read config data from external bundle
            String strEndpointMethod   = env.getProperty("endpoint.stations.method");
            String strEndpointProtocol = env.getProperty("endpoint.stations.protocol");
            String strEndpointHost     = env.getProperty("endpoint.stations.host");
            String strEndpointPort     = env.getProperty("endpoint.stations.port");
            String strEndpointPath     = env.getProperty("endpoint.stations.path");

            LOG.debug("Read config:"+
                    "  endpoint.stations.protocol='"+strEndpointProtocol+"'"+
                    "  endpoint.stations.method='"+strEndpointMethod+"'"+
                    "  endpoint.stations.host='"+strEndpointHost+"'"+
                    "  endpoint.stations.port='"+strEndpointPort+"'"+
                    "  endpoint.stations.path='"+strEndpointPath+"'");

            //Create HTTP Client
            endpointMethodStations   = DCUtils.allowNulls(strEndpointMethod).trim();
            String  endpointProtocol = "http".equalsIgnoreCase(DCUtils.allowNulls(strEndpointProtocol).trim()) ? "http" : "https";
            String  defaultPort      = "http".equalsIgnoreCase(DCUtils.allowNulls(strEndpointProtocol).trim()) ? "80" : "443";
            String  endpointHost = DCUtils.mustNotEndWithSlash(DCUtils.allowNulls(strEndpointHost).trim());
            String  endpointPath = DCUtils.mustNotEndWithSlash(DCUtils.allowNulls(strEndpointPath).trim());
            Integer endpointPort = DCUtils.convertStringToInteger(DCUtils.defaultNulls(strEndpointPort, defaultPort));
            serviceUrlStations = endpointProtocol + "://" + endpointHost + ":" + endpointPort + "/" + endpointPath;

            //Set CookieSpecs.STANDARD to avoid a possible warning 'Invalid cookie header: "Set-Cookie: expires=...'
            Builder requestConfigBuilder = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD);
            RequestConfig requestConfig = requestConfigBuilder.build();
            HttpClientBuilder builderStations = HttpClients.custom();
            builderStations.setDefaultRequestConfig(requestConfig);
            clientStations = builderStations.build();

            LOG.debug("Http Client Stations created");
        }

        if ( clientMeasurements == null ) {
            //Read config data from external bundle
            String strEndpointMethod   = env.getProperty("endpoint.measurements.method");
            String strEndpointProtocol = env.getProperty("endpoint.measurements.protocol");
            String strEndpointHost     = env.getProperty("endpoint.measurements.host");
            String strEndpointPort     = env.getProperty("endpoint.measurements.port");
            String strEndpointPath     = env.getProperty("endpoint.measurements.path");

            LOG.debug("Read config:"+
                    "  endpoint.measurements.protocol='"+strEndpointProtocol+"'"+
                    "  endpoint.measurements.method='"+strEndpointMethod+"'"+
                    "  endpoint.measurements.host='"+strEndpointHost+"'"+
                    "  endpoint.measurements.port='"+strEndpointPort+"'"+
                    "  endpoint.measurements.path='"+strEndpointPath+"'");

            //Create HTTP Client
            endpointMethodMeasurements = DCUtils.allowNulls(strEndpointMethod).trim();
            String  endpointProtocol = "http".equalsIgnoreCase(DCUtils.allowNulls(strEndpointProtocol).trim()) ? "http" : "https";
            String  defaultPort      = "http".equalsIgnoreCase(DCUtils.allowNulls(strEndpointProtocol).trim()) ? "80" : "443";
            String  endpointHost = DCUtils.mustNotEndWithSlash(DCUtils.allowNulls(strEndpointHost).trim());
            String  endpointPath = DCUtils.mustNotEndWithSlash(DCUtils.allowNulls(strEndpointPath).trim());
            Integer endpointPort = DCUtils.convertStringToInteger(DCUtils.defaultNulls(strEndpointPort, defaultPort));
            serviceUrlMeasurements = endpointProtocol + "://" + endpointHost + ":" + endpointPort + "/" + endpointPath;

            //Set CookieSpecs.STANDARD to avoid a possible warning 'Invalid cookie header: "Set-Cookie: expires=...'
            Builder requestConfigBuilder = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD);
            RequestConfig requestConfig = requestConfigBuilder.build();
            HttpClientBuilder builderMeasurements = HttpClients.custom();
            builderMeasurements.setDefaultRequestConfig(requestConfig);
            clientMeasurements = builderMeasurements.build();

            LOG.debug("Http Client Measurements created");
        }
    }

    /**
     * Performs the call to ECOSPAZIO service and returns exactly the response String without particular processing or formatting
     *
     * @return
     * @throws Exception
     */
    private String callRemoteService(CloseableHttpClient client, String serviceUrl, String endpointMethod, List<NameValuePair> endpointParams) throws Exception {
        String url = serviceUrl;
        LOG.debug("Start call to service: " + url);

        HttpRequestBase request = null;
        if ( "GET".equalsIgnoreCase(endpointMethod) ) {
            request = new HttpGet(url);
        } else {
            request = new HttpPost(url);
        }

        // We must add headers for token authorization
        String authToken = converter.getAuthToken();
        if (DCUtils.paramNotNull(authToken)) {
            request.setHeader("Token", authToken);
        }
        request.setHeader("Accept", "application/json");

        URIBuilder uriBuilder = new URIBuilder(request.getURI());
        if ( endpointParams!=null && !endpointParams.isEmpty() ) {
            uriBuilder.addParameters(endpointParams);
        }
        URI uri = uriBuilder.build();
        request.setURI(uri);

        LOG.debug("URI = " + uri);

        CloseableHttpResponse response = client.execute(request);
        StatusLine statusLine = response.getStatusLine();
        if ( response.getStatusLine()==null || statusLine.getStatusCode()!=HttpStatus.SC_OK ) {
            LOG.error("FAILED Call to service "+url+"  Status line is "+statusLine);
        }
        InputStream entity = response.getEntity().getContent();
        StringWriter writer = new StringWriter();
        IOUtils.copy(entity, writer, StandardCharsets.UTF_8);
        String responseData = writer.toString();
        response.close();

        if (LOG.isDebugEnabled()) {
            LOG.debug("End call responseData = '" + responseData + "'");
        }
        return responseData;
    }

//    /**
//     * Converts the string returned by the Bikesharing "/cars/{id}" service in a more useful internal representation
//     *
//     * @param responseString
//     * @return
//     * @throws Exception
//     */
//    public List<BikesharingBzBayDto> convertCarDetailsResponseToInternalDTO(String responseString) throws Exception {
//        List<BikesharingBzBayDto> dtoList = new ArrayList<BikesharingBzBayDto>();
//
//
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("dtoList: "+dtoList);
//        }
//        return dtoList;
//    }

    /**
     * Fetch anagrafic data from ECOSPAZIO service for all stations.
     * Fetch also availability information for each Bike.
     *
     * @return
     * @throws Exception
     */
    public BikesharingBzDto fetchData() throws Exception {
        LOG.info("START.fetchData");
        BikesharingBzDto retval = new BikesharingBzDto();
        try {
            StringBuilder err = new StringBuilder();
            long tsNow = System.currentTimeMillis();

            String responseStringStations = callRemoteService(clientStations, serviceUrlStations, endpointMethodStations, null);

            //Convert to internal representation
            retval = converter.convertStationsResponseToInternalDTO(responseStringStations);
            List<BikesharingBzStationDto> stationList = retval.getStationList();

            //Make a loop on all stations to get detail data
            for (BikesharingBzStationDto stationDto : stationList) {

                //Call service to get availability for each station / bay
                String stationId = stationDto.getId();
                String stationUrl = serviceUrlMeasurements.replace(ServiceCallParam.FUNCTION_NAME_STATION_ID, stationId);
                String responseStringStationDetail = callRemoteService(clientMeasurements, stationUrl, endpointMethodMeasurements, null);
                BikesharingBzStationDto stationDetailDto = converter.convertStationDetailResponseToInternalDTO(responseStringStationDetail);

                //Update detail attributes
                stationDto.setMeasurementTimestamp(tsNow);
                stationDto.setTotalBays        (stationDetailDto.getTotalBays        ());
                stationDto.setFreeBays         (stationDetailDto.getFreeBays         ());
                stationDto.setAvailableVehicles(stationDetailDto.getAvailableVehicles());
                stationDto.setBayList          (stationDetailDto.getBayList());

            }

            if ( (stationList==null || stationList.isEmpty()) && err.length()>0 ) {
                throw new RuntimeException("NO DATA FETCHED: "+err);
            }
        } catch (Exception ex) {
            LOG.error("ERROR in fetchData: " + ex.getMessage(), ex);
            throw ex;
        }
        LOG.info("END.fetchData");
        return retval;
    }

}
