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
@Type("rao-request")
@JsonDeserialize(builder = RaoRequest.RaoRequestBuilder.class)
public final class RaoRequest {

    @Id
    private final String id;
    private final String instant;
    private final String networkFileUrl;
    private final String cracFileUrl;
    private final String refprogFileUrl;
    private final String realGlskFileUrl;
    private final String raoParametersFileUrl;
    private final String virtualhubsFileUrl;
    private final String resultsDestination;
    private final Instant targetEndInstant;
    private final String eventPrefix;

    private RaoRequest(RaoRequestBuilder builder) {
        this.id = builder.id;
        this.instant = builder.instant;
        this.networkFileUrl = builder.networkFileUrl;
        this.cracFileUrl = builder.cracFileUrl;
        this.refprogFileUrl = builder.refprogFileUrl;
        this.realGlskFileUrl = builder.realGlskFileUrl;
        this.raoParametersFileUrl = builder.raoParametersFileUrl;
        this.virtualhubsFileUrl = builder.virtualhubsFileUrl;
        this.resultsDestination = builder.resultsDestination;
        this.targetEndInstant = builder.targetEndInstant;
        this.eventPrefix = builder.eventPrefix;
    }

    public static class RaoRequestBuilder {
        private String id;
        private String instant;
        private String networkFileUrl;
        private String cracFileUrl;
        private String refprogFileUrl;
        private String realGlskFileUrl;
        private String raoParametersFileUrl;
        private String virtualhubsFileUrl;
        private String resultsDestination;
        private Instant targetEndInstant;
        private String eventPrefix;

        @JsonProperty("id")
        public RaoRequestBuilder withId(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty("instant")
        public RaoRequestBuilder withInstant(String instant) {
            this.instant = instant;
            return this;
        }

        @JsonProperty("networkFileUrl")
        public RaoRequestBuilder withNetworkFileUrl(String networkFileUrl) {
            this.networkFileUrl = networkFileUrl;
            return this;
        }

        @JsonProperty("cracFileUrl")
        public RaoRequestBuilder withCracFileUrl(String cracFileUrl) {
            this.cracFileUrl = cracFileUrl;
            return this;
        }

        @JsonProperty("refprogFileUrl")
        public RaoRequestBuilder withRefprogFileUrl(String refprogFileUrl) {
            this.refprogFileUrl = refprogFileUrl;
            return this;
        }

        @JsonProperty("realGlskFileUrl")
        public RaoRequestBuilder withRealGlskFileUrl(String realGlskFileUrl) {
            this.realGlskFileUrl = realGlskFileUrl;
            return this;
        }

        @JsonProperty("raoParametersFileUrl")
        public RaoRequestBuilder withRaoParametersFileUrl(String raoParametersFileUrl) {
            this.raoParametersFileUrl = raoParametersFileUrl;
            return this;
        }

        @JsonProperty("virtualhubsFileUrl")
        public RaoRequestBuilder withVirtualhubsFileUrl(String virtualhubsFileUrl) {
            this.virtualhubsFileUrl = virtualhubsFileUrl;
            return this;
        }

        @JsonProperty("resultsDestination")
        public RaoRequestBuilder withResultsDestination(String resultsDestination) {
            this.resultsDestination = resultsDestination;
            return this;
        }

        @JsonProperty("targetEndInstant")
        public RaoRequestBuilder withTargetEndInstant(Instant targetEndInstant) {
            this.targetEndInstant = targetEndInstant;
            return this;
        }

        @JsonProperty("eventPrefix")
        public RaoRequestBuilder withEventPrefix(String eventPrefix) {
            this.eventPrefix = eventPrefix;
            return this;
        }

        @JsonCreator
        public RaoRequest build() {
            return new RaoRequest(this);
        }
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

    public Optional<String> getVirtualhubsFileUrl() {
        return Optional.ofNullable(virtualhubsFileUrl);
    }

    public Optional<String> getResultsDestination() {
        return Optional.ofNullable(resultsDestination);
    }

    public Optional<Instant> getTargetEndInstant() {
        return Optional.ofNullable(targetEndInstant);
    }

    public Optional<String> getEventPrefix() {
        return Optional.ofNullable(eventPrefix);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
