/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.jasminb.jsonapi.annotations.Id;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@JsonDeserialize(builder = AbstractRaoRequest.AbstractRaoRequestBuilder.class)
public abstract class AbstractRaoRequest {

    @Id
    private final String id;
    private final String runId;
    private final String instant;
    private final String raoParametersFileUrl;
    private final String resultsDestination;
    private final Instant targetEndInstant;
    private final String eventPrefix;

    protected AbstractRaoRequest(AbstractRaoRequestBuilder builder) {
        this.id = builder.id;
        this.runId = builder.runId;
        this.instant = builder.instant;
        this.raoParametersFileUrl = builder.raoParametersFileUrl;
        this.resultsDestination = builder.resultsDestination;
        this.targetEndInstant = builder.targetEndInstant;
        this.eventPrefix = builder.eventPrefix;
    }

    public abstract static class AbstractRaoRequestBuilder<T extends AbstractRaoRequestBuilder> {
        private String id;
        private String runId;
        private String instant;
        private String raoParametersFileUrl;
        private String resultsDestination;
        private Instant targetEndInstant;
        private String eventPrefix;

        @JsonProperty("id")
        public T withId(String id) {
            this.id = id;
            return (T) this;
        }

        @JsonProperty("runId")
        public T withRunId(String runId) {
            this.runId = runId;
            return (T) this;
        }

        @JsonProperty("instant")
        public T withInstant(String instant) {
            this.instant = instant;
            return (T) this;
        }

        @JsonProperty("raoParametersFileUrl")
        public T withRaoParametersFileUrl(String raoParametersFileUrl) {
            this.raoParametersFileUrl = raoParametersFileUrl;
            return (T) this;
        }

        @JsonProperty("resultsDestination")
        public T withResultsDestination(String resultsDestination) {
            this.resultsDestination = resultsDestination;
            return (T) this;
        }

        @JsonProperty("targetEndInstant")
        public T withTargetEndInstant(Instant targetEndInstant) {
            this.targetEndInstant = targetEndInstant;
            return (T) this;
        }

        @JsonProperty("eventPrefix")
        public T withEventPrefix(String eventPrefix) {
            this.eventPrefix = eventPrefix;
            return (T) this;
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
}
