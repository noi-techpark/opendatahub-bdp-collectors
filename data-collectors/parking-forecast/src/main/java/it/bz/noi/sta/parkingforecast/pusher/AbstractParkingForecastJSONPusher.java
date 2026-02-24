// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

/*
 *  Abstract Parking Forecast Json Pusher
 *
 *  (C) 2021 NOI Techpark Südtirol / Alto Adige
 *
 *  changelog:
 *  2022-02-23  1.0 - thomas.nocker@catch-solve.tech
 */

package it.bz.noi.sta.parkingforecast.pusher;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;
import it.bz.noi.sta.parkingforecast.configuration.ParkingForecstConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

public abstract class AbstractParkingForecastJSONPusher extends NonBlockingJSONPusher {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractParkingForecastJSONPusher.class);

    protected String origin;

    @Autowired
    protected ParkingForecstConfiguration connectorConfiguration;

    @Value("${provenance.name}")
    private String provenanceName;

    @Value("${provenance.version}")
    private String provenanceVersion;

    public <T> DataMapDto<RecordDtoImpl> mapData(T arg0) {
        throw new IllegalStateException("it is used by who?");
    }

    @PostConstruct
    public void init() {
        LOG.info("start init");
        origin = connectorConfiguration.getOrigin();
        super.init();
        client = client.mutate().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)).build();
        LOG.info("end init");
    }

    @Override
    public ProvenanceDto defineProvenance() {
        return new ProvenanceDto(null, provenanceName, provenanceVersion, origin);
    }
}
