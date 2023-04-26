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

import java.time.Instant;
import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Type("rao-response")
public class RaoResponse {

    @Id
    private String id;
    private String instant;
    private String networkWithPraFileUrl;
    private String cracFileUrl;
    private String raoResultFileUrl;
    private Instant computationStartInstant;
    private Instant computationEndInstant;
    private boolean interrupted;

    @JsonCreator
    public RaoResponse(@JsonProperty("id") String id,
                       @JsonProperty("instant") String instant,
                       @JsonProperty("networkWithPraFileUrl") String networkWithPraFileUrl,
                       @JsonProperty("cracFileUrl") String cracFileUrl,
                       @JsonProperty("raoResultFileUrl") String raoResultFileUrl,
                       @JsonProperty("computationStartInstant") Instant computationStartInstant,
                       @JsonProperty("computationEndInstant") Instant computationEndInstant,
                       @JsonProperty("interrupted") boolean interrupted) {
        this.id = id;
        this.instant = instant;
        this.networkWithPraFileUrl = networkWithPraFileUrl;
        this.cracFileUrl = cracFileUrl;
        this.raoResultFileUrl = raoResultFileUrl;
        this.computationStartInstant = computationStartInstant;
        this.computationEndInstant = computationEndInstant;
        this.interrupted = interrupted;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getInstant() {
        return Optional.ofNullable(instant);
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

    public Instant getComputationStartInstant() {
        return computationStartInstant;
    }

    public Instant getComputationEndInstant() {
        return computationEndInstant;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
