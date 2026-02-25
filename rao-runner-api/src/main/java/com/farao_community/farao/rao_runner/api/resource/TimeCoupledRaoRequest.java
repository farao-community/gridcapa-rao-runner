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
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Type("rao-request")
@JsonDeserialize(builder = TimeCoupledRaoRequest.RaoRequestBuilder.class)
public final class TimeCoupledRaoRequest extends AbstractRaoRequest {

    private final String icsFileUrl;
    private final String timedInputsFileUrl;

    private TimeCoupledRaoRequest(RaoRequestBuilder builder) {
        super(builder);
        this.icsFileUrl = builder.icsFileUrl;
        this.timedInputsFileUrl = builder.timedInputsFileUrl;
    }

    public static class RaoRequestBuilder extends AbstractRaoRequestBuilder<RaoRequestBuilder> {
        private String icsFileUrl;
        private String timedInputsFileUrl;

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

        @JsonCreator
        public TimeCoupledRaoRequest build() {
            return new TimeCoupledRaoRequest(this);
        }
    }

    public String getIcsFileUrl() {
        return icsFileUrl;
    }

    public String getTimedInputsFileUrl() {
        return timedInputsFileUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
