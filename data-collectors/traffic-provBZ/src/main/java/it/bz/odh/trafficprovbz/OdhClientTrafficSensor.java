// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.trafficprovbz;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class OdhClientTrafficSensor extends NonBlockingJSONPusher {

	@Value("${odh_client.provenance.name}")
	private String provenanceName;

	@Value("${odh_client.provenance.version}")
	private String provenanceVersion;

	@Value("${odh_client.provenance.origin}")
	private String provenanceOrigin;

	private String stationtype = "TrafficSensor";

	@Override
	public <T> DataMapDto<RecordDtoImpl> mapData(T data) {
		/* You can ignore this legacy method call */
		return null;
	}

	@Override
	public String initIntegreenTypology() {
		return stationtype;
	}

	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null, provenanceName, provenanceVersion, provenanceOrigin);
	}

	public ProvenanceDto getProvenance() {
		return provenance;
	}

}
