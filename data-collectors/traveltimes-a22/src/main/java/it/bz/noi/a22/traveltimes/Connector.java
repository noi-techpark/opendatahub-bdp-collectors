// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

/*
 *  A22 travel times API connector
 *
 *  (C) 2029 NOI Techpark Südtirol / Alto Adige
 *
 *
 *  changelog:
 *  	2020-02-26 chris@1006.org
 *
 *
 *  upstream API reference:
 *   	2017-002 Autostrada del Brennero S.p.A. - Manutenzione ordinaria e straordinaria sistema di supervisione CAU - anno 2017
 *  	No doc: 2017-0046
 *  	Revisione: 3
 *
 */
package it.bz.noi.a22.traveltimes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * encapsulates the A22 API to query travel times
 */
public class Connector {

    private static final String USER_AGENT = "NOI/A22TravelTimesConnector";
    private static final int WS_CONN_TIMEOUT_MSEC = 30000;
    private static final int WS_READ_TIMEOUT_MSEC = 1800000;
	private static final Logger LOG = LoggerFactory.getLogger(MainA22Traveltimes.class);

    private String token = null;
    private String url = null;

    /**
     * get a new authentication token and store it in the instance
     *
     * @param url
     * @param username
     * @param password
     *
     * @throws java.io.IOException
     *
     */
    public Connector(String url, String username, String password) throws IOException {

        String auth_json = "{\"request\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}";

        // make authentication request
        HttpURLConnection conn = (HttpURLConnection) (new URL(url + "/token")).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(WS_CONN_TIMEOUT_MSEC);
        conn.setReadTimeout(WS_READ_TIMEOUT_MSEC);
        conn.setDoOutput(true);
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
        os.write(auth_json + "\n");
        os.flush();
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("authentication failure (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        os.close();
        conn.disconnect();

        // parse response and store authentication token
        String session_id = null;
        try {
            JSONObject response_json = (JSONObject) JSONValue.parse(response.toString());
            JSONObject subscribe_result = (JSONObject) response_json.get("SubscribeResult");
            session_id = (String) subscribe_result.get("sessionId");
        } catch (Exception e) {
            // null pointer or cast exception in case the JSON hasn't the expected form
            e.printStackTrace();
            throw new RuntimeException("authentication failure (could not parse response)");
        }

        if (session_id == null) {
            throw new RuntimeException("authentication failure (could not find sessionId in response)");
        }

        this.url = url;
        this.token = session_id;

        LOG.debug("authentication    OK new token = " + token);

    }

    /**
     * release the authentication token
     *
     * @throws IOException
     */
    public void close() throws IOException {

        if (url == null || token == null) {
            throw new RuntimeException("de-authentication failure (currently not authenticated)");
        }

        // make de-authentication request
        HttpURLConnection conn = (HttpURLConnection) (new URL(url + "/token/" + token)).openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(WS_CONN_TIMEOUT_MSEC);
        conn.setReadTimeout(WS_READ_TIMEOUT_MSEC);
        conn.setDoOutput(true);
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
        os.write("\n");
        os.flush();
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("de-authentication failure (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        os.close();
        conn.disconnect();

        // parse response
        Boolean result = null;
        try {
            JSONObject response_json = (JSONObject) JSONValue.parse(response.toString());
            result = (Boolean) response_json.get("RemoveSubscribeResult");
        } catch (Exception e) {
            // null pointer or cast exception in case the JSON hasn't the expected form
            e.printStackTrace();
            throw new RuntimeException("de-authentication failure (could not parse response)");
        }
        if (result == null || !result) {
            throw new RuntimeException("de-authentication failure (de-authentication was not confirmed)");
        }

        LOG.debug("de-authentication OK old token = " + token);

        this.url = null;
        this.token = null;

    }

    /**
     * get info about the segments for which travel times are measured
     *
     * @return an ArrayList of HashMaps with the info about all segments
     *
     * @throws IOException
     */
    public List<HashMap<String, String>> getTravelTimeSegments() throws IOException {

        if (url == null || token == null) {
            throw new RuntimeException("not authenticated");
        }

        HttpURLConnection conn = (HttpURLConnection) (new URL(url + "/percorrenze/anagrafica")).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(WS_CONN_TIMEOUT_MSEC);
        conn.setReadTimeout(WS_READ_TIMEOUT_MSEC);
        conn.setDoOutput(true);
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
        os.write("{\"sessionId\":\"" + token + "\"}\n");
        os.flush();
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("could not get travel time segments (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        os.close();
        conn.disconnect();

        // parse response
        ArrayList<HashMap<String, String>> output = new ArrayList<>();
        try {
            JSONObject response_json = (JSONObject) JSONValue.parse(response.toString());
            JSONArray segment_list = (JSONArray) response_json.get("Percorrenze_GetTrattiResult");
            int i;
            for (i = 0; i < segment_list.size(); i++) {
                JSONObject segment = (JSONObject) segment_list.get(i);
                // note we return all fields as strings, but will type check the numbers
                HashMap<String, String> h = new HashMap<>();
                h.put("autostrada", (String)segment.get("autostrada"));
                h.put("idtratto", (String)segment.get("idtratto"));
                h.put("descrizione", (String)segment.get("descrizione"));
                try
                {
                   h.put("latitudineinizio", String.valueOf(segment.get("latitudineinizio")));
                   h.put("longitudininizio", String.valueOf(segment.get("longitudininizio")));
                   h.put("latitudinefine", String.valueOf(segment.get("latitudinefine")));
                   h.put("longitudinefine", String.valueOf(segment.get("longitudinefine")));
                }
                catch (Exception exxx)
                {
                   continue;
                }
                h.put("iddirezione", String.valueOf(segment.get("iddirezione")));
                h.put("metroinizio", String.valueOf(segment.get("metroinizio")));
                h.put("metrofine", String.valueOf(segment.get("metrofine")));
                output.add(h);
            }
        } catch (Exception e) {
            // null pointer, cast or number format exception in case the JSON hasn't the expected form or data types
            e.printStackTrace();
            throw new RuntimeException("could not parse list of segments");
        }

        LOG.debug("getTravelTimeSegments() OK: got " + output.size() + " segments");

        return output;
    }
    
    /**
     * get the travel times for the given segment ID ("idtratto") within the given time interval
     *
     * @param fr - Unix epoch in UTC indicating the lower bound of the interval
     *
     * @param to - Unix epoch in UTC indicating the upper bound of the interval (the interval *includes* this time stamp)
     *
     * @param id - segment ID ("idtratto") - leave empty to get travel times for all segments
     *
     * @return an ArrayList of HashMaps with the info about travel times
     *
     * @throws IOException
     */
    public List<HashMap<String, String>> getTravelTimes(long fr, long to, String id) throws IOException {

        if (url == null || token == null) {
            throw new RuntimeException("not authenticated");
        }

        List<HashMap<String, String>> output = new ArrayList<>();

        HashMap<Integer, Integer> http_codes = new HashMap<>();

        // convert to format used by A22
        // (see the comment "Reverse engineering the A22 time stamp format" at the end of the file)
        String frTS = fr + "000+0000";
        String toTS = to + "999+0000";
        
        LOG.debug("Retrieving traveltimes for station {} with timestamps {} - {}", id, frTS, toTS);

        // retrieve events
        HttpURLConnection conn = (HttpURLConnection) (new URL(url + "/percorrenze/tempi")).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(WS_CONN_TIMEOUT_MSEC);
        conn.setReadTimeout(WS_READ_TIMEOUT_MSEC);
        conn.setDoOutput(true);
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
        String reqBody = "{\"request\":{\"sessionId\":\"" + token + "\",\"idtratto\":\"" + id + "\",\"fromData\":\"/Date(" + frTS + ")/\",\"toData\":\"/Date(" + toTS + ")/\"}}\n";
        os.write(reqBody);
        os.flush();
        int status = conn.getResponseCode();
        if (http_codes.containsKey(status)) {
            http_codes.put(status, http_codes.get(status) + 1);
        } else {
            http_codes.put(status, 1);
        }
        if (status != 200) {
            try{
                LOG.error("Request body: " + reqBody);
            } catch (Exception e) {} //don't care
                            
            // The API returns HTTP 500 if the timeframe requested is too old.
            // For initial imports set this env variable true and HTTP 500s are ignored. This should enable the data collector to catch up
            // Once caught up, disable it again, to properly fail on actual server errors
            if (status == 500 && "true".equalsIgnoreCase(System.getenv("IGNORE_500"))){
                LOG.warn("Got HTTP 500, but ignoring it...");
                return output;
            }

            throw new RuntimeException("could not get travel times (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        os.close();
        conn.disconnect();

        // parse response
        try {
            JSONObject response_json = (JSONObject) JSONValue.parse(response.toString());
            JSONArray traveltimes_list = (JSONArray) response_json.get("Percorrenze_GetTempiResult");
            int i;
            for (i = 0; i < traveltimes_list.size(); i++) {
                JSONObject traveltime = (JSONObject) traveltimes_list.get(i);
                // note we return all fields as strings, but will type check the numbers
                HashMap<String, String> h = new HashMap<>();
                h.put("idtratto", (String)traveltime.get("idtratto"));
                // convert from format used by A22
                // (see the comment "Reverse engineering the A22 time stamp format" at the end of the file)
                h.put("data", String.valueOf(Long.valueOf(((String)traveltime.get("data")).substring(6, 16))));

                h.put("lds", (String)traveltime.get("lds"));
                h.put("tempo", String.valueOf(traveltime.get("tempo")));
                // "velocita" can come as Long or Double from the JSON API, we parse it as Double
                h.put("velocita", String.valueOf(Double.valueOf(String.valueOf(traveltime.get("velocita")))));

                h.put("pesanti_lds", (String)traveltime.get("lds"));
                h.put("pesanti_tempo", String.valueOf(traveltime.get("tempo")));
                // "velocita" can come as Long or Double from the JSON API, we parse it as Double
                h.put("pesanti_velocita", String.valueOf(Double.valueOf(String.valueOf(traveltime.get("velocita")))));
                output.add(h);
            }
        } catch (Exception e) {
            // null pointer, cast or number format exception in case the JSON hasn't the expected form or data types
            e.printStackTrace();
            throw new RuntimeException("could not parse list of travel times");
        }

        LOG.debug("getTravelTimes() OK: got " + output.size() + " travel times");

        return output;

    }

    /**
     * get info about the TVCC segments for which travel times are measured
     *
     * @return an ArrayList of HashMaps with the info about all TVCC segments
     *
     * @throws IOException
     */
    public List<HashMap<String, String>> getTVCCSegments() throws IOException {

        if (url == null || token == null) {
            throw new RuntimeException("not authenticated");
        }

        HttpURLConnection conn = (HttpURLConnection) (new URL(url + "/percorrenze/anagraficaTVCC")).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(WS_CONN_TIMEOUT_MSEC);
        conn.setReadTimeout(WS_READ_TIMEOUT_MSEC);
        conn.setDoOutput(true);
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
        os.write("{\"sessionId\":\"" + token + "\"}\n");
        os.flush();
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("could not get TVCC segments (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        os.close();
        conn.disconnect();

        // parse response
        ArrayList<HashMap<String, String>> output = new ArrayList<>();
        try {
            JSONObject response_json = (JSONObject) JSONValue.parse(response.toString());
            JSONArray segment_list = (JSONArray) response_json.get("Percorrenze_GetTrattiTVCCResult");
            int i;
            for (i = 0; i < segment_list.size(); i++) {
                JSONObject segment = (JSONObject) segment_list.get(i);
                HashMap<String, String> h = new HashMap<>();
                h.put("autostrada", (String)segment.get("autostrada"));
                h.put("idtratto", (String)segment.get("idtratto"));
                h.put("descrizione", (String)segment.get("descrizione"));
                if (segment.get("descrizioneD") != null) {
                    h.put("descrizioneD", (String)segment.get("descrizioneD"));
                }
                try
                {
                   h.put("latitudineinizio", String.valueOf(segment.get("latitudineinizio")));
                   h.put("longitudininizio", String.valueOf(segment.get("longitudininizio")));
                   h.put("latitudinefine", String.valueOf(segment.get("latitudinefine")));
                   h.put("longitudinefine", String.valueOf(segment.get("longitudinefine")));
                }
                catch (Exception exxx)
                {
                   continue;
                }
                h.put("iddirezione", String.valueOf(segment.get("iddirezione")));
                h.put("metroinizio", String.valueOf(segment.get("metroinizio")));
                h.put("metrofine", String.valueOf(segment.get("metrofine")));
                output.add(h);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("could not parse list of TVCC segments");
        }

        LOG.debug("getTVCCSegments() OK: got " + output.size() + " segments");

        return output;
    }

    /**
     * get the TVCC travel times for the given segment ID ("idtratto") within the given time interval
     *
     * @param fr - Unix epoch in UTC indicating the lower bound of the interval
     * @param to - Unix epoch in UTC indicating the upper bound of the interval
     * @param id - segment ID ("idtratto") - leave empty to get travel times for all segments
     *
     * @return an ArrayList of HashMaps with the info about TVCC travel times
     *
     * @throws IOException
     */
    public List<HashMap<String, String>> getTVCCTravelTimes(long fr, long to, String id) throws IOException {

        if (url == null || token == null) {
            throw new RuntimeException("not authenticated");
        }

        List<HashMap<String, String>> output = new ArrayList<>();

        HashMap<Integer, Integer> http_codes = new HashMap<>();

        String frTS = fr + "000+0000";
        String toTS = to + "999+0000";

        LOG.debug("Retrieving TVCC traveltimes for station {} with timestamps {} - {}", id, frTS, toTS);

        HttpURLConnection conn = (HttpURLConnection) (new URL(url + "/percorrenze/tempiTVCC")).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(WS_CONN_TIMEOUT_MSEC);
        conn.setReadTimeout(WS_READ_TIMEOUT_MSEC);
        conn.setDoOutput(true);
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
        String reqBody = "{\"request\":{\"sessionId\":\"" + token + "\",\"idtratto\":\"" + id + "\",\"fromData\":\"/Date(" + frTS + ")/\",\"toData\":\"/Date(" + toTS + ")/\"}}\n";
        os.write(reqBody);
        os.flush();
        int status = conn.getResponseCode();
        if (http_codes.containsKey(status)) {
            http_codes.put(status, http_codes.get(status) + 1);
        } else {
            http_codes.put(status, 1);
        }
        if (status != 200) {
            try{
                LOG.error("Request body: " + reqBody);
            } catch (Exception e) {}

            if (status == 500 && "true".equalsIgnoreCase(System.getenv("IGNORE_500"))){
                LOG.warn("Got HTTP 500, but ignoring it...");
                return output;
            }

            throw new RuntimeException("could not get TVCC travel times (response code was " + status + ")");
        }

        // get response
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            response.append(s);
        }
        os.close();
        conn.disconnect();

        // parse response
        try {
            JSONObject response_json = (JSONObject) JSONValue.parse(response.toString());
            JSONArray traveltimes_list = (JSONArray) response_json.get("Percorrenze_GetTempiTVCCResult");
            int i;
            for (i = 0; i < traveltimes_list.size(); i++) {
                JSONObject traveltime = (JSONObject) traveltimes_list.get(i);
                HashMap<String, String> h = new HashMap<>();
                h.put("idtratto", (String)traveltime.get("idtratto"));
                h.put("data", String.valueOf(Long.valueOf(((String)traveltime.get("data")).substring(6, 16))));

                h.put("lds", (String)traveltime.get("lds"));
                h.put("tempo", String.valueOf(traveltime.get("tempo")));
                h.put("velocita", String.valueOf(Double.valueOf(String.valueOf(traveltime.get("velocita")))));

                h.put("pesanti_lds", (String)traveltime.get("pesanti_lds"));
                h.put("pesanti_tempo", String.valueOf(traveltime.get("pesanti_tempo")));
                h.put("pesanti_velocita", String.valueOf(Double.valueOf(String.valueOf(traveltime.get("pesanti_velocita")))));

                h.put("num_leggeri", String.valueOf(traveltime.get("num_leggeri")));
                h.put("num_pesanti", String.valueOf(traveltime.get("num_pesanti")));
                output.add(h);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("could not parse list of TVCC travel times");
        }

        LOG.debug("getTVCCTravelTimes() OK: got " + output.size() + " travel times");

        return output;

    }

    /*

    Reverse engineering the A22 time stamp format
    --------------------------------------------

    In CET we have daylight saving change from +2 to +1
    on 28 oct 2018 (the clock goes from 3:00 to 2:00).

    These are events from one sensor around the daylight change:

    [...]
    /Date(1540688331000+0200)/
    /Date(1540688331000+0200)/
    /Date(1540688358000+0200)/
    /Date(1540688358000+0200)/
    /Date(1540688390000+0200)/
    /Date(1540688560000+0100)/
    /Date(1540688645000+0100)/
    /Date(1540688645000+0100)/
    /Date(1540688648000+0100)/
    /Date(1540688656000+0100)/
    [...]

    /Date(1540688390000+0200)/

        $ date --date='@1540688390'
        Sun Oct 28 00:59:50 UTC 2018

        00:59 UTC would be 02:59 CET ~ 03 CET


    /Date(1540688560000+0100)/

        $ date --date='@1540688560'
        Sun Oct 28 01:02:40 UTC 2018

        01:02 UTC would be 02:02 CET ~ 02 CET

    That means the first part of this string *is* the correct time stamp
    in unix epoch / UTC.

     */
}
