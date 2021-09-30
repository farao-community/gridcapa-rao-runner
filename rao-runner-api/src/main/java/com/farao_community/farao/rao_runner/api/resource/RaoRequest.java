/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Type("rao-request")
public class RaoRequest {

    @Id
    private final String id;
    private final String instant;
    private final String networkFileUrl;
    private final String cracFileUrl;
    private final String raoParametersFileUrl;
    private final String refprogFileUrl;
    private final String realGlskFileUrl;
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
                      @JsonProperty("cracFileUrl") String cracFileUrl,
                      @JsonProperty("raoParametersFileUrl") String raoParametersFileUrl) {
        this(id, null, networkFileUrl, cracFileUrl, null, null, raoParametersFileUrl, null);
    }

    public RaoRequest(@JsonProperty("id") String id,
                      @JsonProperty("networkFileUrl") String networkFileUrl,
                      @JsonProperty("cracFileUrl") String cracFileUrl,
                      @JsonProperty("raoParametersFileUrl") String raoParametersFileUrl,
                      @JsonProperty("resultsDestination") String resultsDestination) {
        this(id, null, networkFileUrl, cracFileUrl, null, null, raoParametersFileUrl, resultsDestination);
    }

    public String getId() {
        return id;
    }

    public Optional<String> getInstant() {
        return Optional.ofNullable(instant);
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

    public String getRaoParametersFileUrl() {
        return raoParametersFileUrl;
    }

    public Optional<String> getResultsDestination() {
        return Optional.ofNullable(resultsDestination);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
