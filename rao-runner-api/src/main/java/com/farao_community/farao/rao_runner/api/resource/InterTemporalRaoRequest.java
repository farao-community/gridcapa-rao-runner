/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Type("rao-request")
@JsonDeserialize(builder = InterTemporalRaoRequest.RaoRequestBuilder.class)
public final class InterTemporalRaoRequest {

    @Id
    private final String id;
    private final String runId;
    private final String instant;
    private final String icsFileUrl;
    private final String timedInputsFileUrl;
    private final String raoParametersFileUrl;
    private final String resultsDestination;
    private final Instant targetEndInstant;
    private final String eventPrefix;

    private InterTemporalRaoRequest(RaoRequestBuilder builder) {
        this.id = builder.id;
        this.runId = builder.runId;
        this.instant = builder.instant;
        this.icsFileUrl = builder.icsFileUrl;
        this.timedInputsFileUrl = builder.timedInputsFileUrl;
        this.raoParametersFileUrl = builder.raoParametersFileUrl;
        this.resultsDestination = builder.resultsDestination;
        this.targetEndInstant = builder.targetEndInstant;
        this.eventPrefix = builder.eventPrefix;
    }

    public static class RaoRequestBuilder {
        private String id;
        private String runId;
        private String instant;
        private String icsFileUrl;
        private String timedInputsFileUrl;
        private String raoParametersFileUrl;
        private String resultsDestination;
        private Instant targetEndInstant;
        private String eventPrefix;

        @JsonProperty("id")
        public RaoRequestBuilder withId(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty("runId")
        public RaoRequestBuilder withRunId(String runId) {
            this.runId = runId;
            return this;
        }

        @JsonProperty("instant")
        public RaoRequestBuilder withInstant(String instant) {
            this.instant = instant;
            return this;
        }

        @JsonProperty("icsFileUrl")
        public RaoRequestBuilder withIcsFileUrl(String icsFileUrl) {
            this.icsFileUrl = icsFileUrl;
            return this;
        }

        @JsonProperty("timedInputsFileUrl")
        public RaoRequestBuilder withTimedInputsFileUrl(String timedInputsFileUrl) {
            this.timedInputsFileUrl = timedInputsFileUrl;
            return this;
        }

        @JsonProperty("raoParametersFileUrl")
        public RaoRequestBuilder withRaoParametersFileUrl(String raoParametersFileUrl) {
            this.raoParametersFileUrl = raoParametersFileUrl;
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
        public InterTemporalRaoRequest build() {
            return new InterTemporalRaoRequest(this);
        }
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public Optional<String> getInstant() {
        return Optional.ofNullable(instant);
    }

    public String getIcsFileUrl() {
        return icsFileUrl;
    }

    public String getTimedInputsFileUrl() {
        return timedInputsFileUrl;
    }

    public String getRaoParametersFileUrl() {
        return raoParametersFileUrl;
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
