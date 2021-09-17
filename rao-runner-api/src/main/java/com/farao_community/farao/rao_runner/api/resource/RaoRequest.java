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

import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@JsonTypeName("rao-request")
public class RaoRequest {

    @JsonIdentityReference(alwaysAsId = true)
    private final String id;
    private final String instant;
    private final String networkFileUrl;
    private final String cracFileUrl;
    private final String refprogFileUrl;
    private final String realGlskFileUrl;
    private final String raoParametersFileUrl;
    private final String resultsDestination;

    @JsonCreator
    public RaoRequest(@JsonProperty("id") String id,
                      @JsonProperty("instant") String instant,
                      @JsonProperty("networkFileUrl") String networkFileUrl,
                      @JsonProperty("cracFileUrl") String cracFileUrl,
                      @JsonProperty("refprogFileUrl") String refprogFileUrl,
                      @JsonProperty("realGlskFileUrl") String realGlskFileUrl,
                      @JsonProperty("raoParametersFileUrl") String raoParametersFileUrl,
                      @JsonProperty("resultsDestination") String resultsDestination) {
        this.id = id;
        this.instant = instant;
        this.networkFileUrl = networkFileUrl;
        this.refprogFileUrl = refprogFileUrl;
        this.cracFileUrl = cracFileUrl;
        this.realGlskFileUrl = realGlskFileUrl;
        this.raoParametersFileUrl = raoParametersFileUrl;
        this.resultsDestination = resultsDestination;
    }

    public RaoRequest(@JsonProperty("id") String id,
                      @JsonProperty("networkFileUrl") String networkFileUrl,
                      @JsonProperty("cracFileUrl") String cracFileUrl) {
        this.id = id;
        this.networkFileUrl = networkFileUrl;
        this.cracFileUrl = cracFileUrl;
        this.instant = null;
        this.refprogFileUrl = null;
        this.realGlskFileUrl = null;
        this.raoParametersFileUrl = null;
        this.resultsDestination = null;
    }

    public String getId() {
        return id;
    }

    public String getInstant() {
        return instant;
    }

    public String getNetworkFileUrl() {
        return networkFileUrl;
    }

    public String getCracFileUrl() {
        return cracFileUrl;
    }

    public Optional<String> getRefprogFileUrl() {
        return Optional.ofNullable(refprogFileUrl);
    }

    public Optional<String> getRealGlskFileUrl() {
        return Optional.ofNullable(realGlskFileUrl);
    }

    public Optional<String> getRaoParametersFileUrl() {
        return Optional.ofNullable(raoParametersFileUrl);
    }

    public Optional<String> getResultsDestination() {
        return Optional.ofNullable(resultsDestination);
    }
}
