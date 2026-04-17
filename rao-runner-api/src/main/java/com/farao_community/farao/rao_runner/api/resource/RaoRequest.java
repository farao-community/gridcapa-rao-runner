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
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Type("rao-request")
@JsonDeserialize(builder = RaoRequest.RaoRequestBuilder.class)
public final class RaoRequest extends AbstractRaoRequest {

    private final String instant;
    private final String networkFileUrl;
    private final String cracFileUrl;
    private final String refprogFileUrl;
    private final String realGlskFileUrl;
    private final String virtualhubsFileUrl;

    private RaoRequest(RaoRequestBuilder builder) {
        super(builder);
        this.instant = builder.instant;
        this.networkFileUrl = builder.networkFileUrl;
        this.cracFileUrl = builder.cracFileUrl;
        this.refprogFileUrl = builder.refprogFileUrl;
        this.realGlskFileUrl = builder.realGlskFileUrl;
        this.virtualhubsFileUrl = builder.virtualhubsFileUrl;
    }

    public static class RaoRequestBuilder extends AbstractRaoRequestBuilder<RaoRequestBuilder> {
        private String instant;
        private String networkFileUrl;
        private String cracFileUrl;
        private String refprogFileUrl;
        private String realGlskFileUrl;
        private String virtualhubsFileUrl;

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

        @JsonProperty("virtualhubsFileUrl")
        public RaoRequestBuilder withVirtualhubsFileUrl(String virtualhubsFileUrl) {
            this.virtualhubsFileUrl = virtualhubsFileUrl;
            return this;
        }

        @JsonCreator
        public RaoRequest build() {
            return new RaoRequest(this);
        }
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

    public Optional<String> getVirtualhubsFileUrl() {
        return Optional.ofNullable(virtualhubsFileUrl);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
