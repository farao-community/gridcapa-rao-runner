/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Type("rao-response")
@JsonDeserialize(builder = RaoResponse.RaoResponseBuilder.class)
public final class RaoResponse {

    @Id
    private final String id;
    private final String instant;
    private final String networkWithPraFileUrl;
    private final String cracFileUrl;
    private final String raoResultFileUrl;
    private final Instant computationStartInstant;
    private final Instant computationEndInstant;
    private final boolean interrupted;

    private RaoResponse(RaoResponseBuilder builder) {
        this.id = builder.id;
        this.instant = builder.instant;
        this.networkWithPraFileUrl = builder.networkWithPraFileUrl;
        this.cracFileUrl = builder.cracFileUrl;
        this.raoResultFileUrl = builder.raoResultFileUrl;
        this.computationStartInstant = builder.computationStartInstant;
        this.computationEndInstant = builder.computationEndInstant;
        this.interrupted = builder.interrupted;
    }

    public static class RaoResponseBuilder {
        private String id;
        private String instant;
        private String networkWithPraFileUrl;
        private String cracFileUrl;
        private String raoResultFileUrl;
        private Instant computationStartInstant;
        private Instant computationEndInstant;
        private  boolean interrupted;

        @JsonProperty("id")
        public RaoResponseBuilder withId(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty("instant")
        public RaoResponseBuilder withInstant(String instant) {
            this.instant = instant;
            return this;
        }

        @JsonProperty("networkWithPraFileUrl")
        public RaoResponseBuilder withNetworkWithPraFileUrl(String networkWithPraFileUrl) {
            this.networkWithPraFileUrl = networkWithPraFileUrl;
            return this;
        }

        @JsonProperty("cracFileUrl")
        public RaoResponseBuilder withCracFileUrl(String cracFileUrl) {
            this.cracFileUrl = cracFileUrl;
            return this;
        }

        @JsonProperty("raoResultFileUrl")
        public RaoResponseBuilder withRaoResultFileUrl(String raoResultFileUrl) {
            this.raoResultFileUrl = raoResultFileUrl;
            return this;
        }

        @JsonProperty("computationStartInstant")
        public RaoResponseBuilder withComputationStartInstant(Instant computationStartInstant) {
            this.computationStartInstant = computationStartInstant;
            return this;
        }

        @JsonProperty("computationEndInstant")
        public RaoResponseBuilder withComputationEndInstant(Instant computationEndInstant) {
            this.computationEndInstant = computationEndInstant;
            return this;
        }


        @JsonProperty("interrupted")
        public RaoResponseBuilder withInterrupted(boolean interrupted) {
            this.interrupted = interrupted;
            return this;
        }

        @JsonCreator
        public RaoResponse build() {
            return new RaoResponse(this);
        }
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
