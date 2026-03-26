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

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Type("time-coupled-rao-response")
@JsonDeserialize(builder = TimeCoupledRaoSuccessResponse.Builder.class)
public final class TimeCoupledRaoSuccessResponse extends AbstractRaoResponse {

    @Id
    private final String id;
    private final String networksWithPraFileUrl;
    private final String raoResultsFileUrl;
    private final Instant computationStartInstant;
    private final Instant computationEndInstant;
    private final boolean interrupted;

    private TimeCoupledRaoSuccessResponse(Builder builder) {
        this.id = builder.id;
        this.networksWithPraFileUrl = builder.networksWithPraFileUrl;
        this.raoResultsFileUrl = builder.raoResultsFileUrl;
        this.computationStartInstant = builder.computationStartInstant;
        this.computationEndInstant = builder.computationEndInstant;
        this.interrupted = builder.interrupted;
        this.raoFailed = false;
    }

    public static class Builder {
        private String id;
        private String networksWithPraFileUrl;
        private String raoResultsFileUrl;
        private Instant computationStartInstant;
        private Instant computationEndInstant;
        private  boolean interrupted;

        @JsonProperty("id")
        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty("networksWithPraFileUrl")
        public Builder withNetworksWithPraFileUrl(String networksWithPraFileUrl) {
            this.networksWithPraFileUrl = networksWithPraFileUrl;
            return this;
        }

        @JsonProperty("raoResultsFileUrl")
        public Builder withRaoResultsFileUrl(String raoResultsFileUrl) {
            this.raoResultsFileUrl = raoResultsFileUrl;
            return this;
        }

        @JsonProperty("computationStartInstant")
        public Builder withComputationStartInstant(Instant computationStartInstant) {
            this.computationStartInstant = computationStartInstant;
            return this;
        }

        @JsonProperty("computationEndInstant")
        public Builder withComputationEndInstant(Instant computationEndInstant) {
            this.computationEndInstant = computationEndInstant;
            return this;
        }

        @JsonProperty("interrupted")
        public Builder withInterrupted(boolean interrupted) {
            this.interrupted = interrupted;
            return this;
        }

        @JsonCreator
        public TimeCoupledRaoSuccessResponse build() {
            return new TimeCoupledRaoSuccessResponse(this);
        }
    }

    public String getId() {
        return id;
    }

    public String getNetworksWithPraFileUrl() {
        return networksWithPraFileUrl;
    }

    public String getRaoResultsFileUrl() {
        return raoResultsFileUrl;
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
