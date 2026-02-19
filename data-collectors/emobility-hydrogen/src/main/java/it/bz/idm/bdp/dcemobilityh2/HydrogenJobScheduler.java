// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcemobilityh2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import it.bz.idm.bdp.dcemobilityh2.dto.HydrogenDto;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationList;

/**
 * Cronjob configuration can be found under src/main/resources/META-INF/spring/applicationContext.xml
 * XXX Do not forget to configure it!
 */
@Component
public class HydrogenJobScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(HydrogenJobScheduler.class.getName());

	@Autowired
	private Environment env;

	@Autowired
	private HydrogenDataPusher pusher;

	@Autowired
	private HydrogenDataRetriever retrieval;

	private String plugTypeName;
	private static final List<DataTypeDto> EMOBILTYTYPES = new ArrayList<DataTypeDto>() {
		private static final long serialVersionUID = 1L;

		{
			add(new DataTypeDto("number-available","","number of available vehicles / charging points","Instantaneous"));
			add(new DataTypeDto("echarging-plug-status","","the state can either be 0, which means that the plug is currently not available, or it can be 1 which means it is",""));
		}
	};
	@PostConstruct
	private void init() {
		LOG.debug("START.");

		plugTypeName    = env.getProperty(HydrogenDataConverter.PLUG_TYPE_KEY);

		LOG.debug("END.");
	}

	/** JOB 1 */
	public void pushStations() throws Exception {
		LOG.debug("START.pushStations");

		try {
			List<HydrogenDto> data = retrieval.fetchData();

			StationList stations = pusher.mapStations2Bdp(data);
			StationList plugs    = pusher.mapPlugs2Bdp(data);
			if (stations != null && plugs != null) {
				pusher.syncStations(stations);
				pusher.syncStations(plugTypeName, plugs);
			}
		} catch (HttpClientErrorException e) {
			LOG.error(pusher + " - " + e + " - " + e.getResponseBodyAsString(), e);
		}
		LOG.debug("END.pushStations");
	}
	public void syncTypes() {
		pusher.syncDataTypes(EMOBILTYTYPES);
	}
	/** JOB 2 */
	public void pushData() throws Exception {
		LOG.debug("START.pushStations");

		try {
			List<HydrogenDto> data = retrieval.fetchData();

			DataMapDto<RecordDtoImpl> map = pusher.mapData(data);
			DataMapDto<RecordDtoImpl> plugRec = pusher.mapPlugData2Bdp(data);

			if (map != null && plugRec != null){
				pusher.pushData(map);
				pusher.pushData(plugTypeName,plugRec);
			}

		} catch (Exception e) {
			LOG.error(pusher + " - " + e, e);
			throw e;
		}
		LOG.debug("END.pushStations");
	}
}