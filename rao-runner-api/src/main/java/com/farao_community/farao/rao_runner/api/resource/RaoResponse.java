/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@JsonTypeName("rao-response")
public class RaoResponse {

    @JsonIdentityReference(alwaysAsId = true)
    private final String id;
    private final String instant;
    private final String networkWithPraFileUrl;
    private final String cracFileUrl;
    private final String raoResultFileUrl;

    @JsonCreator
    public RaoResponse(@JsonProperty("id") String id,
                       @JsonProperty("instant") String instant,
                       @JsonProperty("networkWithPraFileUrl") String networkWithPraFileUrl,
                       @JsonProperty("cracFileUrl") String cracFileUrl,
                       @JsonProperty("raoResultFileUrl") String raoResultFileUrl) {
        this.id = id;
        this.instant = instant;
        this.networkWithPraFileUrl = networkWithPraFileUrl;
        this.cracFileUrl = cracFileUrl;
        this.raoResultFileUrl = raoResultFileUrl;
    }

    public String getId() {
        return id;
    }

    public String getInstant() {
        return instant;
    }

    public String getNetworkWithPraFileUrl() {
        return networkWithPraFileUrl;
    }

    public String getCracFileUrl() {
        return cracFileUrl;
    }

    public String getRaoResultFileUrl() {
        return raoResultFileUrl;
    }
}
