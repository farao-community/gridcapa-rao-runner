/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api;

import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoSuccessResponse;
import com.farao_community.farao.rao_runner.api.resource.TimedInput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class JsonApiConverterTest {
    public static final TimedInput TIMED_INPUT = new TimedInput(OffsetDateTime.parse("2025-12-31T12:34Z"), "networkFileUrl", "cracFileUrl");

    @Test
    void checkRaoRequestJsonConversion() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoRequestMessage.json").readAllBytes();
        RaoRequest raoRequest = jsonConverter.fromJsonMessage(requestBytes, RaoRequest.class);
        Assertions.assertThat(raoRequest.getId()).isEqualTo("id");
        Assertions.assertThat(raoRequest.getInstant()).contains("instant");
        Assertions.assertThat(raoRequest.getNetworkFileUrl()).isEqualTo("networkFileUrl");
        Assertions.assertThat(raoRequest.getCracFileUrl()).isEqualTo("cracFileUrl");
        Assertions.assertThat(raoRequest.getRefprogFileUrl()).contains("refprogFileUrl");
        Assertions.assertThat(raoRequest.getRealGlskFileUrl()).contains("realGlskFileUrl");
        Assertions.assertThat(raoRequest.getRaoParametersFileUrl()).isEqualTo("raoParametersFileUrl");
        Assertions.assertThat(raoRequest.getVirtualhubsFileUrl()).contains("virtualhubsFileUrl");
        Assertions.assertThat(raoRequest.getResultsDestination()).contains("resultsDestination");
        Assertions.assertThat(raoRequest.getTargetEndInstant()).contains(Instant.ofEpochSecond(1637052884, 944727000));
        Assertions.assertThat(raoRequest.getEventPrefix().isEmpty()).isTrue();
    }

    private void roundTripTestOnRaoRequest(RaoRequest raoRequest) {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        RaoRequest importedRaoRequest = jsonConverter.fromJsonMessage(jsonConverter.toJsonMessage(raoRequest), RaoRequest.class);

        Assertions.assertThat(importedRaoRequest.getId()).isEqualTo(raoRequest.getId());
        Assertions.assertThat(importedRaoRequest.getInstant()).isEqualTo(raoRequest.getInstant());
        Assertions.assertThat(importedRaoRequest.getNetworkFileUrl()).isEqualTo(raoRequest.getNetworkFileUrl());
        Assertions.assertThat(importedRaoRequest.getCracFileUrl()).isEqualTo(raoRequest.getCracFileUrl());
        Assertions.assertThat(importedRaoRequest.getRefprogFileUrl()).isEqualTo(raoRequest.getRefprogFileUrl());
        Assertions.assertThat(importedRaoRequest.getRealGlskFileUrl()).isEqualTo(raoRequest.getRealGlskFileUrl());
        Assertions.assertThat(importedRaoRequest.getRaoParametersFileUrl()).isEqualTo(raoRequest.getRaoParametersFileUrl());
        Assertions.assertThat(importedRaoRequest.getResultsDestination()).isEqualTo(raoRequest.getResultsDestination());
        Assertions.assertThat(importedRaoRequest.getTargetEndInstant()).isEqualTo(raoRequest.getTargetEndInstant());
    }

    @Test
    void roundTripTest() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .withRefprogFileUrl("refprogFileUrl")
                .withRealGlskFileUrl("glskFileUrl")
                .withRaoParametersFileUrl("raoParametersFileUrl")
                .withResultsDestination("resultsDestination")
                .withTargetEndInstant(Instant.ofEpochSecond(1637052884, 944727000))
                .withEventPrefix("eventPrefix")
                .build();

        roundTripTestOnRaoRequest(raoRequest);
    }

    @Test
    void roundTripTestWithEmptyOptionals() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .withRefprogFileUrl("refprogFileUrl")
                .build();
        roundTripTestOnRaoRequest(raoRequest);
    }

    @Test
    void checkRaoRequestJsonConversionWithEmptyOptionals() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoRequestMessageEmptyOptionals.json").readAllBytes();
        RaoRequest raoRequest = jsonConverter.fromJsonMessage(requestBytes, RaoRequest.class);
        Assertions.assertThat(raoRequest.getId()).isEqualTo("id");
        Assertions.assertThat(raoRequest.getInstant()).contains("instant");
        Assertions.assertThat(raoRequest.getNetworkFileUrl()).isEqualTo("networkFileUrl");
        Assertions.assertThat(raoRequest.getCracFileUrl()).isEqualTo("cracFileUrl");
        Assertions.assertThat(raoRequest.getRaoParametersFileUrl()).isEqualTo("raoParametersFileUrl");
        Assertions.assertThat(raoRequest.getRefprogFileUrl()).isEmpty();
        Assertions.assertThat(raoRequest.getRealGlskFileUrl()).isEmpty();
        Assertions.assertThat(raoRequest.getResultsDestination()).isEmpty();
        Assertions.assertThat(raoRequest.getTargetEndInstant()).isEmpty();
        Assertions.assertThat(raoRequest.getEventPrefix()).isEmpty();
    }

    @Test
    void checkRaoResponseJsonConversion() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoResponseMessage.json").readAllBytes();
        RaoSuccessResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, RaoSuccessResponse.class);
        Assertions.assertThat(raoResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoResponse.getInstant()).contains("instant");
        Assertions.assertThat(raoResponse.getNetworkWithPraFileUrl()).isEqualTo("networkWithPraFileUrl");
        Assertions.assertThat(raoResponse.getCracFileUrl()).isEqualTo("cracFileUrl");
        Assertions.assertThat(raoResponse.getRaoResultFileUrl()).isEqualTo("raoResultFileUrl");
        Assertions.assertThat(raoResponse.getComputationStartInstant()).isEqualTo(Instant.ofEpochSecond(1637052884, 944727000));
        Assertions.assertThat(raoResponse.getComputationEndInstant()).isEqualTo(Instant.ofEpochSecond(1647057884, 934927000));
    }

    @Test
    void checkRaoResponseJsonConversionWhithNullInstant() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoResponseMessageNullInstant.json").readAllBytes();
        RaoSuccessResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, RaoSuccessResponse.class);
        Assertions.assertThat(raoResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoResponse.getInstant()).isEmpty();
        Assertions.assertThat(raoResponse.getNetworkWithPraFileUrl()).isEqualTo("networkWithPraFileUrl");
        Assertions.assertThat(raoResponse.getCracFileUrl()).isEqualTo("cracFileUrl");
        Assertions.assertThat(raoResponse.getRaoResultFileUrl()).isEqualTo("raoResultFileUrl");
        Assertions.assertThat(raoResponse.getComputationStartInstant()).isNull();
        Assertions.assertThat(raoResponse.getComputationEndInstant()).isNull();
    }

    @Test
    void checkTimeCoupledRaoRequestJsonConversion() throws IOException {
        final JsonApiConverter jsonConverter = new JsonApiConverter();
        final byte[] requestBytes = getClass().getResourceAsStream("/timeCoupledRaoRequestMessage.json").readAllBytes();

        final TimeCoupledRaoRequest raoRequest = jsonConverter.fromJsonMessage(requestBytes, TimeCoupledRaoRequest.class);

        Assertions.assertThat(raoRequest.getId()).isEqualTo("id");
        Assertions.assertThat(raoRequest.getInstant()).contains("instant");
        Assertions.assertThat(raoRequest.getIcsFileUrl()).isEqualTo("icsFileUrl");
        Assertions.assertThat(raoRequest.getTimedInputs()).isEqualTo(List.of(TIMED_INPUT));
        Assertions.assertThat(raoRequest.getRaoParametersFileUrl()).isEqualTo("raoParametersFileUrl");
        Assertions.assertThat(raoRequest.getResultsDestination()).contains("resultsDestination");
        Assertions.assertThat(raoRequest.getTargetEndInstant()).contains(Instant.ofEpochSecond(1637052884, 944727000));
        Assertions.assertThat(raoRequest.getEventPrefix()).isEmpty();
    }

    private void roundTripTestOnTimeCoupledRaoRequest(final TimeCoupledRaoRequest raoRequest) {
        final JsonApiConverter jsonConverter = new JsonApiConverter();
        final TimeCoupledRaoRequest importedRaoRequest = jsonConverter.fromJsonMessage(jsonConverter.toJsonMessage(raoRequest), TimeCoupledRaoRequest.class);

        Assertions.assertThat(importedRaoRequest.getId()).isEqualTo(raoRequest.getId());
        Assertions.assertThat(importedRaoRequest.getInstant()).isEqualTo(raoRequest.getInstant());
        Assertions.assertThat(importedRaoRequest.getIcsFileUrl()).isEqualTo(raoRequest.getIcsFileUrl());
        Assertions.assertThat(importedRaoRequest.getTimedInputs()).isEqualTo(raoRequest.getTimedInputs());
        Assertions.assertThat(importedRaoRequest.getRaoParametersFileUrl()).isEqualTo(raoRequest.getRaoParametersFileUrl());
        Assertions.assertThat(importedRaoRequest.getResultsDestination()).isEqualTo(raoRequest.getResultsDestination());
        Assertions.assertThat(importedRaoRequest.getTargetEndInstant()).isEqualTo(raoRequest.getTargetEndInstant());
    }

    @Test
    void timeCoupledRoundTripTest() {
        final TimeCoupledRaoRequest raoRequest = new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("id")
            .withInstant("instant")
            .withIcsFileUrl("icsFileUrl")
            .withTimedInputs(List.of(TIMED_INPUT))
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .withResultsDestination("resultsDestination")
            .withTargetEndInstant(Instant.ofEpochSecond(1637052884, 944727000))
            .withEventPrefix("eventPrefix")
            .build();

        roundTripTestOnTimeCoupledRaoRequest(raoRequest);
    }

    @Test
    void timeCoupledRoundTripTestWithEmptyOptionals() {
        final TimeCoupledRaoRequest raoRequest = new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("id")
            .withInstant("instant")
            .withIcsFileUrl("icsFileUrl")
            .withTimedInputs(List.of(TIMED_INPUT))
            .build();

        roundTripTestOnTimeCoupledRaoRequest(raoRequest);
    }

    @Test
    void checkTimeCoupledRaoRequestJsonConversionWithEmptyOptionals() throws IOException {
        final JsonApiConverter jsonConverter = new JsonApiConverter();
        final byte[] requestBytes = getClass().getResourceAsStream("/timeCoupledRaoRequestMessageEmptyOptionals.json").readAllBytes();

        final TimeCoupledRaoRequest raoRequest = jsonConverter.fromJsonMessage(requestBytes, TimeCoupledRaoRequest.class);

        Assertions.assertThat(raoRequest.getId()).isEqualTo("id");
        Assertions.assertThat(raoRequest.getInstant()).contains("instant");
        Assertions.assertThat(raoRequest.getIcsFileUrl()).isEqualTo("icsFileUrl");
        Assertions.assertThat(raoRequest.getTimedInputs()).isEqualTo(List.of(TIMED_INPUT));
        Assertions.assertThat(raoRequest.getRaoParametersFileUrl()).isEqualTo("raoParametersFileUrl");
        Assertions.assertThat(raoRequest.getResultsDestination()).isEmpty();
        Assertions.assertThat(raoRequest.getTargetEndInstant()).isEmpty();
        Assertions.assertThat(raoRequest.getEventPrefix()).isEmpty();
    }

    @Test
    void checkTimeCoupledRaoResponseJsonConversion() throws IOException {
        final JsonApiConverter jsonConverter = new JsonApiConverter();
        final byte[] requestBytes = getClass().getResourceAsStream("/timeCoupledRaoResponseMessage.json").readAllBytes();

        final TimeCoupledRaoSuccessResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, TimeCoupledRaoSuccessResponse.class);

        Assertions.assertThat(raoResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoResponse.getInstant()).contains("instant");
        Assertions.assertThat(raoResponse.getNetworksWithPraFileUrl()).isEqualTo("networksWithPraFileUrl");
        Assertions.assertThat(raoResponse.getRaoResultsFileUrl()).isEqualTo("raoResultsFileUrl");
        Assertions.assertThat(raoResponse.getComputationStartInstant()).isEqualTo(Instant.ofEpochSecond(1637052884, 944727000));
        Assertions.assertThat(raoResponse.getComputationEndInstant()).isEqualTo(Instant.ofEpochSecond(1647057884, 934927000));
    }

    @Test
    void checkTimeCoupledRaoResponseJsonConversionWhithNullInstant() throws IOException {
        final JsonApiConverter jsonConverter = new JsonApiConverter();
        final byte[] requestBytes = getClass().getResourceAsStream("/timeCoupledRaoResponseMessageNullInstant.json").readAllBytes();

        final TimeCoupledRaoSuccessResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, TimeCoupledRaoSuccessResponse.class);

        Assertions.assertThat(raoResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoResponse.getInstant()).isEmpty();
        Assertions.assertThat(raoResponse.getNetworksWithPraFileUrl()).isEqualTo("networksWithPraFileUrl");
        Assertions.assertThat(raoResponse.getRaoResultsFileUrl()).isEqualTo("raoResultsFileUrl");
        Assertions.assertThat(raoResponse.getComputationStartInstant()).isNull();
        Assertions.assertThat(raoResponse.getComputationEndInstant()).isNull();
    }
}
