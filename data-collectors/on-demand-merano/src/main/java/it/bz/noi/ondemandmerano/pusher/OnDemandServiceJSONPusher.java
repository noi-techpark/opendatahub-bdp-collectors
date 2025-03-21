// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.pusher;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;
import it.bz.noi.ondemandmerano.configuration.OnDemandMeranoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

public abstract class OnDemandServiceJSONPusher extends NonBlockingJSONPusher {

    private static final Logger LOG = LoggerFactory.getLogger(OnDemandServiceJSONPusher.class);

    protected String origin;

    @Autowired
    protected Environment env;
    @Autowired
    protected OnDemandMeranoConfiguration onDemandMeranoConfiguration;

    public <T> DataMapDto<RecordDtoImpl> mapData(T arg0) {
        throw new IllegalStateException("it is used by who?");
    }

    @PostConstruct
    public void init() {
        LOG.info("start init");
        origin = onDemandMeranoConfiguration.getOrigin();
        super.init();
        LOG.info("end init");
    }

    @Override
    public ProvenanceDto defineProvenance() {
        return new ProvenanceDto(null, env.getProperty("provenance_name"), env.getProperty("provenance_version"), origin);
    }
}
