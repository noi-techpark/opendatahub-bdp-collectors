// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcemobilityh2;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import it.bz.idm.bdp.dcemobilityh2.dto.HydrogenDto;

@Component
@PropertySource({ "classpath:/META-INF/spring/application.properties" })
public class HydrogenDataRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(HydrogenDataRetriever.class.getName());

    @Autowired
    private Environment env;

    @Autowired
    private HydrogenDataConverter converter;

    private HttpClientBuilder builder = HttpClients.custom();
    private CloseableHttpClient client;
    private XmlMapper mapper = new XmlMapper();

    private String endpointMethod;
    private String endpointProtocol;
    private String endpointHost;
    private Integer endpointPort;
    private String endpointPath;
    private String strEndpointParams;
    private List<NameValuePair> endpointParams;
    private String serviceUrl;

    public HydrogenDataRetriever() {
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
            endpointParams = new ArrayList<NameValuePair>();
            StringBuffer sb = new StringBuffer();
            strEndpointParams = null;

            int i=0;
            while ( strEndpointParams == null ) {
                String key = DCUtils.allowNulls(env.getProperty("endpoint.param."+i+".key"));
                String val = DCUtils.allowNulls(env.getProperty("endpoint.param."+i+".value"));
                if ( DCUtils.paramNotNull(key) ) {
                    endpointParams.add(new BasicNameValuePair(key, val));
                    if ( i > 0 ) {
                        sb.append("&");
                    }
                    sb.append(key).append("=").append(val);
                    i++;
                } else {
                    strEndpointParams = sb.toString();
                }
            }

            LOG.debug("Read config:"+
                    "  endpoint.protocol='"+strEndpointProtocol+"'  endpoint.method='"+strEndpointMethod+"'  endpoint.host='"+strEndpointHost+"+"+
                    "  endpoint.port='"+strEndpointPort+"'  endpoint.path='"+strEndpointPath+"'  endpoint.params='"+strEndpointParams+"'");

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
     * Performs the call to IIT service and returns exactly the response String without particular processing or formatting
     *
     * @return
     * @throws Exception
     */
    private String callRemoteService() throws Exception {
        LOG.debug("Start call to service: " + serviceUrl);

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
            request = new HttpGet(serviceUrl);
        } else {
            request = new HttpPost(serviceUrl);
        }
        URI uri = new URIBuilder(request.getURI()).addParameters(endpointParams).build();
        request.setURI(uri);

        CloseableHttpResponse response = client.execute(request); //client.execute(endPoint, get, localContext);
        InputStream entity = response.getEntity().getContent();
        StringWriter writer = new StringWriter();
        IOUtils.copy(entity, writer);
        String responseData = writer.toString();
        response.close();

        LOG.debug("End call responseData = '" + responseData + "'");
        return responseData;
    }

    /**
     * Converts the string returned by the IIT service in a more useful internal representation
     *
     * @param responseString
     * @return
     * @throws Exception
     */
    public List<HydrogenDto> convertResponseToInternalDTO(String responseString) throws Exception {
        List<Map<String, String>> dataMap = mapper.readValue(responseString, new TypeReference<List<Map<String, String>>>() {});
        LOG.debug("dataMap: "+dataMap);
        if ( dataMap == null ) {
            return null;
        }
        List<HydrogenDto> dtoList = converter.convertToInternalDTO(dataMap);

        LOG.debug("dtoList: "+dtoList);
        return dtoList;
    }

    /**
     * Fetch data from where you want, to be integrated into the Open Data Hub.
     * Insert logging for debugging and errors if needed, but do not prevent
     * exceptions from being thrown to not hide any malfunctioning.
     *
     * @throws Exception
     *             on error propagate exception to caller
     */
    public List<HydrogenDto> fetchData() throws Exception {
        LOG.debug("START.fetchData");
        List<HydrogenDto> dtoList = null;
        try {
            String responseString = callRemoteService();

			// removes paymenttypes from XML response
			String cleanedString = responseString.replaceAll("<paymenttypes>[\\s\\S]*?</paymenttypes>","");
            dtoList = convertResponseToInternalDTO(cleanedString);
        } catch (Exception ex) {
            LOG.error("ERROR in fetchData: " + ex.getMessage(), ex);
            throw ex;
        }
        LOG.debug("END.fetchData");
        return dtoList;
    }

}
