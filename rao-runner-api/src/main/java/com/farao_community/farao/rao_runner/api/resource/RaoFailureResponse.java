/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Type("rao-response")
@JsonDeserialize(builder = RaoFailureResponse.Builder.class)
public final class RaoFailureResponse extends AbstractRaoResponse {

    @Id
    private final String id;
    private final String errorMessage;

    private RaoFailureResponse(Builder builder) {
        this.id = builder.id;
        this.errorMessage = builder.errorMessage;
        this.raoFailed = true;
    }

    public static class Builder {
        private String id;
        private String errorMessage;

        @JsonProperty("id")
        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty("errorMessage")
        public Builder withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @JsonCreator
        public RaoFailureResponse build() {
            return new RaoFailureResponse(this);
        }
    }

    public String getId() {
        return id;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
