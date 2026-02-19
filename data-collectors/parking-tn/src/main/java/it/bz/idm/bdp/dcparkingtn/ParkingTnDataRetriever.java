// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcparkingtn;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.bz.idm.bdp.dcparkingtn.dto.ParkingAreaServiceDto;
import it.bz.idm.bdp.dcparkingtn.dto.ParkingTnDto;

@Component
@PropertySource({ "classpath:/META-INF/spring/application.properties" })
public class ParkingTnDataRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingTnDataRetriever.class.getName());

    @Autowired
    private Environment env;

    @Autowired
    private ParkingTnDataConverter converter;

    private HttpClientBuilder builder = HttpClients.custom();
    private CloseableHttpClient client;
    private ObjectMapper mapper = new ObjectMapper();

    private String endpointMethod;
    private String endpointProtocol;
    private String endpointHost;
    private Integer endpointPort;
    private String endpointPath;
    private List<Map<String, String>> cityProps;
    private String serviceUrl;

    public ParkingTnDataRetriever() {
        LOG.debug("Create instance");
    }

    @PostConstruct
    private void initClient(){
        LOG.debug("Init");
        if (client==null) {

            //Read config data from external bundle
            String strEndpointMethod   = env.getProperty("endpoint.method");
            String strEndpointProtocol = env.getProperty("endpoint.protocol");
            String strEndpointHost     = env.getProperty("endpoint.host");
            String strEndpointPort     = env.getProperty("endpoint.port");
            String strEndpointPath     = env.getProperty("endpoint.path");

            cityProps = new ArrayList<Map<String, String>>();
            boolean hasNext = true;

            int i=0;
            while ( hasNext ) {
                String key = DCUtils.allowNulls(env.getProperty("endpoint.city."+i+"."+ParkingTnDataConverter.STATION_KEY_PARAM)).trim();
                String codePrefix = DCUtils.allowNulls(env.getProperty("endpoint.city."+i+"."+ParkingTnDataConverter.STATION_CODE_PREFIX_PARAM)).trim();
                if ( DCUtils.paramNotNull(key) ) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put(ParkingTnDataConverter.STATION_KEY_PARAM, key);
                    map.put(ParkingTnDataConverter.STATION_CODE_PREFIX_PARAM, codePrefix);
                    cityProps.add(map);
                    i++;
                } else {
                    hasNext = false;
                }
            }

            LOG.debug("Read config:"+
                    "  endpoint.protocol='"+strEndpointProtocol+"'  endpoint.method='"+strEndpointMethod+"'  endpoint.host='"+strEndpointHost+"+"+
                    "  endpoint.port='"+strEndpointPort+"'  endpoint.path='"+strEndpointPath+"'  endpoint.cityProps='"+cityProps+"'");

            //Create HTTP Client
            endpointMethod   = DCUtils.allowNulls(strEndpointMethod).trim();
            endpointProtocol = "http".equalsIgnoreCase(DCUtils.allowNulls(strEndpointProtocol).trim()) ? "http" : "https";
            endpointHost = DCUtils.mustNotEndWithSlash(DCUtils.allowNulls(strEndpointHost).trim());
            endpointPath = DCUtils.mustNotEndWithSlash(DCUtils.allowNulls(strEndpointPath).trim());
            endpointPort = DCUtils.convertStringToInteger(DCUtils.defaultNulls(strEndpointPort, "443"));
            serviceUrl = endpointProtocol + "://" + endpointHost + ":" + endpointPort + "/" + endpointPath;

            client = builder.build();

            LOG.debug("Http Client created");
        }
    }

    /**
     * Performs the call to Parking service and returns exactly the response String without particular processing or formatting
     * 
     * @return
     * @throws Exception
     */
    private String callRemoteService(String pathInfo) throws Exception {
        String url = serviceUrl + "/" + pathInfo;
        LOG.debug("Start call to service: " + url);

        // In our case it is not necessary to set particular headers
//        String xcallerHeader = env.getProperty("app.callerId");
//        String apikey = env.getProperty("app.apikey");
//        if (xcallerHeader != null)
//            get.setHeader("X-Caller-ID",xcallerHeader);
//        if (apikey != null)
//            get.setHeader("apikey",apikey);
//        get.setHeader("Accept","application/json");

        HttpRequestBase request = null;
        if ( "GET".equalsIgnoreCase(endpointMethod) ) {
            request = new HttpGet(url);
        } else {
            request = new HttpPost(url);
        }
        URI uri = new URIBuilder(request.getURI()).build();
        request.setURI(uri);

        CloseableHttpResponse response = client.execute(request);
        InputStream entity = response.getEntity().getContent();
        StringWriter writer = new StringWriter();
        IOUtils.copy(entity, writer);
        String responseData = writer.toString();
        response.close();

        LOG.debug("End call responseData = '" + responseData + "'");
        return responseData;
    }

    /**
     * Converts the string returned by the Parking service in a more useful internal representation
     * 
     * @param responseString
     * @return
     * @throws Exception
     */
    public List<ParkingTnDto> convertResponseToInternalDTO(String responseString, String municipality, String codePrefix) throws Exception {
        List<ParkingAreaServiceDto> dataList = mapper.readValue(responseString, new TypeReference<List<ParkingAreaServiceDto>>() {});
        LOG.debug("dataList: "+dataList);
        if ( dataList == null ) {
            return null;
        }
        List<ParkingTnDto> dtoList = new ArrayList<ParkingTnDto>();
        dtoList = converter.convertToInternalDTO(dataList, municipality, codePrefix);

        LOG.debug("dtoList: "+dtoList); 
        return dtoList;
    }

    /**
     * Fetch data from Parking service, to be integrated into the Open Data Hub.
     * A loop on all provided cities is performed:
     *    for every city a call to the parking service is done and at the end 
     *    all data is collected in a single list.
     * Do not prevent exceptions from being thrown to not hide any malfunctioning.
     *
     * @throws Exception
     *             on error propagate exception to caller
     */
    public List<ParkingTnDto> fetchData() throws Exception {
        LOG.debug("START.fetchData");
        List<ParkingTnDto> dtoList = new ArrayList<ParkingTnDto>();
        try {
            StringBuffer err = new StringBuffer();
            for (Map<String, String> city : cityProps) {
                String key = city.get(ParkingTnDataConverter.STATION_KEY_PARAM);
                String codePrefix = city.get(ParkingTnDataConverter.STATION_CODE_PREFIX_PARAM);
                //We do not stop execution if one call goes in exception.
                //All exceptions are collected in a StringBuffer
                try {
                    List<ParkingTnDto> dataByCity = fetchDataByCity(key, codePrefix);
                    dtoList.addAll(dataByCity);
                } catch (Exception ex) {
                    LOG.error("ERROR in fetchData: " + ex.getMessage(), ex);
                    String stackTrace = DCUtils.extractStackTrace(ex, -1);
                    err.append("\n***** EXCEPTION RETRIEVING DATA FOR CITY: '"+key+"' ******" + stackTrace);
                }
            }
            if ( dtoList.size()==0 && err.length()>0 ) {
                throw new RuntimeException("NO DATA FETCHED: "+err);
            }
        } catch (Exception ex) {
            LOG.error("ERROR in fetchData: " + ex.getMessage(), ex);
            throw ex;
        }
        LOG.debug("END.fetchData");
        return dtoList;
    }

    /**
     * Fetch data from Parking service for one specific city.
     * 
     * @param cityKey
     * @return
     * @throws Exception
     */
    public List<ParkingTnDto> fetchDataByCity(String cityKey, String codePrefix) throws Exception {
        LOG.debug("START.fetchData("+cityKey+")");
        List<ParkingTnDto> dtoList = null;
        try {
            String responseString = callRemoteService(cityKey);
            dtoList = convertResponseToInternalDTO(responseString, cityKey, codePrefix);
            int size = dtoList!=null ? dtoList.size() : -1;
            LOG.debug("Data fetched for city "+cityKey+": "+size);
        } catch (Exception ex) {
            LOG.error("ERROR in fetchData: " + ex.getMessage(), ex);
            throw ex;
        }
        LOG.debug("END.fetchData("+cityKey+")");
        return dtoList;
    }

}
