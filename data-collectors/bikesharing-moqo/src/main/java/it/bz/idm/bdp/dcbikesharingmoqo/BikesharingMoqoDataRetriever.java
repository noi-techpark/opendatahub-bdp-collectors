// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcbikesharingmoqo;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dcbikesharingmoqo.dto.AvailabilityDto;
import it.bz.idm.bdp.dcbikesharingmoqo.dto.BikeDto;
import it.bz.idm.bdp.dcbikesharingmoqo.dto.BikesharingMoqoDto;
import it.bz.idm.bdp.dcbikesharingmoqo.dto.BikesharingMoqoPageDto;
import it.bz.idm.bdp.dcbikesharingmoqo.dto.LocationDto;
import it.bz.idm.bdp.dcbikesharingmoqo.dto.PaginationDto;

@Component
@PropertySource({ "classpath:/META-INF/spring/application.properties" })
public class BikesharingMoqoDataRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(BikesharingMoqoDataRetriever.class.getName());

    @Autowired
    private Environment env;

    @Autowired
    private BikesharingMoqoDataConverter converter;

    private HttpClientBuilder builderStations;
    private HttpClientBuilder builderMeasurements;

    private CloseableHttpClient clientStations;
    private CloseableHttpClient clientMeasurements;

    private String endpointMethodStations;
    private String serviceUrlStations;
    private List<ServiceCallParam> stationsParams;

    private String endpointMethodMeasurements;
    private String serviceUrlMeasurements;

    public BikesharingMoqoDataRetriever() {
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
            builderStations = HttpClients.custom();
            builderStations.setDefaultRequestConfig(requestConfig);
            clientStations = builderStations.build();

            stationsParams = new ArrayList<ServiceCallParam>();
            boolean hasNext = true;
            int i=0;
            while ( hasNext ) {
                //Example of parameters to add
                //endpoint.stations.param.0.param_name=include_unavailable_cars
                //endpoint.stations.param.0.param_value=true
                String paramName  = DCUtils.allowNulls(env.getProperty("endpoint.stations.param."+i+".param_name")).trim();
                String paramValue = DCUtils.allowNulls(env.getProperty("endpoint.stations.param."+i+".param_value")).trim();
                String functionName = DCUtils.allowNulls(env.getProperty("endpoint.stations.param."+i+".function_name")).trim();
                if ( DCUtils.paramNotNull(paramName) ) {
                    ServiceCallParam param = new ServiceCallParam(paramName);
                    if ( DCUtils.paramNotNull(functionName) ) {
                        param.type = ServiceCallParam.TYPE_FUNCTION;
                        param.value = functionName;
                    } else if ( DCUtils.paramNotNull(paramValue) ) {
                        param.type = ServiceCallParam.TYPE_FIXED_VALUE;
                        param.value = paramValue;
                    }
                    if ( param.type!=null && param.value!=null ) {
                        stationsParams.add(param);
                    } else {
                        LOG.warn("UNRECOGNIZED parameter type in application.properties file: '"+paramName+"'  index="+i+"");
                    }
                    i++;
                } else {
                    hasNext = false;
                }
            }

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
            builderMeasurements = HttpClients.custom();
            builderMeasurements.setDefaultRequestConfig(requestConfig);
            clientMeasurements = builderMeasurements.build();

            LOG.debug("Http Client Measurements created");
        }
    }

    /**
     * Performs the call to MOQO service and returns exactly the response String without particular processing or formatting
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

        // We must add headers for bearer authorization and Selected-team, read them from env property file
        String authToken = converter.getAuthToken();
        String selectedTeam = converter.getSelectedTeam();
        if (DCUtils.paramNotNull(authToken)) {
            request.setHeader("Authorization", authToken);
        }
        if (DCUtils.paramNotNull(selectedTeam)) {
            request.setHeader("X-Selected-Team", selectedTeam);
        }
        request.setHeader("Accept", "application/json");

        URIBuilder uriBuilder = new URIBuilder(request.getURI());
        if ( endpointParams!=null && endpointParams.size()>0 ) {
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
        IOUtils.copy(entity, writer);
        String responseData = writer.toString();
        response.close();

        if (LOG.isDebugEnabled()) {
            LOG.debug("End call responseData = '" + responseData + "'");
        }
        return responseData;
    }

    /**
     * Converts the string returned by the Bikesharing "/cars/{id}" service in a more useful internal representation
     * 
     * @param responseString
     * @return
     * @throws Exception
     */
    public List<BikeDto> convertCarDetailsResponseToInternalDTO(String responseString) throws Exception {
        List<BikeDto> dtoList = new ArrayList<BikeDto>();


        if (LOG.isDebugEnabled()) {
            LOG.debug("dtoList: "+dtoList); 
        }
        return dtoList;
    }

    /**
     * Fetch anagrafic data from MOQO service for all stations.
     * Fetch also availability information for each Bike.
     * 
     * @return
     * @throws Exception
     */
    public BikesharingMoqoDto fetchData() throws Exception {
        LOG.info("START.fetchData");
        BikesharingMoqoDto retval = new BikesharingMoqoDto();
        List<BikeDto> dtoList = new ArrayList<BikeDto>();
        retval.setBikeList(dtoList);
        try {
            StringBuffer err = new StringBuffer();

            //Call service that retrieves the list of stations
            //We call the "/cars" service that returns paginated data. pagination info is stored in structure "pagination" that looks like this: 
            //    "pagination": {"total_pages": 6, "current_page": 1, "next_page": 2, "prev_page": null}
            long pageNum = 1;
            boolean lastPage = false;
            while ( !lastPage ) {

                //Fill endpoint params
                List<NameValuePair> endpointParams = new ArrayList<NameValuePair>();
                if ( stationsParams!=null && stationsParams.size()>0 ) {
                    for (ServiceCallParam entry : stationsParams) {
                        String paramName  = entry.name;
                        String paramValue = null;
                        BasicNameValuePair param = null;

                        //Parameters can be of various type
                        if ( ServiceCallParam.TYPE_FIXED_VALUE.equals(entry.type) ) {
                            //If parameter is of type FIXED_VALUE take the value read from env property
                            paramValue = entry.value;
                            if ( DCUtils.paramNotNull(paramName) && DCUtils.paramNotNull(paramValue) ) {
                                param = new BasicNameValuePair(paramName, paramValue);
                            }
                        } else if ( ServiceCallParam.TYPE_FUNCTION.equals(entry.type) ) {
                            if ( ServiceCallParam.FUNCTION_NAME_PAGE_NUM.equals(entry.value) ) {
                                //If parameter is of type FUNCTION PAGE_NUM take the value from current page to fetch
                                paramValue = String.valueOf(pageNum);
                                if ( DCUtils.paramNotNull(paramName) && DCUtils.paramNotNull(paramValue) ) {
                                    param = new BasicNameValuePair(paramName, paramValue);
                                }
                            }
                        }

                        if ( param != null ) {
                            endpointParams.add(param);
                        }
                    }
                }

                String responseStringCars = callRemoteService(clientStations, serviceUrlStations, endpointMethodStations, endpointParams);

                //Convert to internal representation
                BikesharingMoqoPageDto pageDto = converter.convertCarsResponseToInternalDTO(responseStringCars);
                List<BikeDto> pageList = pageDto.getBikeList();
                PaginationDto pagination = pageDto.getPagination();

                //Make a loop on all bikes to get availability data
                for (BikeDto bikeDto : pageList) {

                    //Call service to get availability for each bike
                    String bikeId = bikeDto.getId();
                    String bikeUrl = serviceUrlMeasurements.replace(ServiceCallParam.FUNCTION_NAME_STATION_ID, bikeId);
                    String responseStringAvail = callRemoteService(clientMeasurements, bikeUrl, endpointMethodMeasurements, null);
                    List<AvailabilityDto> availDtoList = converter.convertAvailabilityResponseToInternalDTO(responseStringAvail);
                    bikeDto.setAvailabilityList(availDtoList);

                    //Evaluate attributes available, until and from for the bike, looking into the Availability slots
                    //This method is currently not used
                    //converter.calculateBikeAvailability_FromUntil(bikeDto, availDtoList);

                    //Evaluate attributes availability and future-availability
                    boolean availabilityDataOk = converter.calculateBikeAvailability(bikeDto, availDtoList);

                    //If availability data is not consistent, we do not put the bike in the return List
                    if ( availabilityDataOk ) {
                        dtoList.add(bikeDto);
                    }
                }

                //Exit if this is the last page, otherwise continue loop with next page
                Long nextPage = pagination.getNextPage();
                if ( nextPage != null ) {
                    pageNum = nextPage;
                } else {
                    lastPage = true;
                }

            }

            Map<String, LocationDto> distinctLocations = converter.getDistinctLocations(dtoList);
            retval.setLocationMap(distinctLocations);

            if ( dtoList.size()==0 && err.length()>0 ) {
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
