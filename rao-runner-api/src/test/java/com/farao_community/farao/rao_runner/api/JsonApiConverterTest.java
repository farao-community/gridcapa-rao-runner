/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api;

import com.farao_community.farao.rao_runner.api.exceptions.AbstractRaoRunnerException;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class JsonApiConverterTest {

    @Test
    void checkRaoRequestJsonConversion() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoRequestMessage.json").readAllBytes();
        RaoRequest raoRequest = jsonConverter.fromJsonMessage(requestBytes, RaoRequest.class);
        assertEquals("id", raoRequest.getId());
        assertEquals("instant", raoRequest.getInstant().get());
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertEquals("cracFileUrl", raoRequest.getCracFileUrl());
        assertEquals("refprogFileUrl", raoRequest.getRefprogFileUrl().get());
        assertEquals("realGlskFileUrl", raoRequest.getRealGlskFileUrl().get());
        assertEquals("raoParametersFileUrl", raoRequest.getRaoParametersFileUrl());
        assertEquals("virtualhubsFileUrl", raoRequest.getVirtualhubsFileUrl().get());
        assertEquals("resultsDestination", raoRequest.getResultsDestination().get());
        assertEquals(Instant.ofEpochSecond(1637052884, 944727000), raoRequest.getTargetEndInstant().get());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
    }

    private void roundTripTestOnRaoRequest(RaoRequest raoRequest) {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        RaoRequest importedRaoRequest = jsonConverter.fromJsonMessage(jsonConverter.toJsonMessage(raoRequest), RaoRequest.class);

        assertEquals(raoRequest.getId(), importedRaoRequest.getId());
        assertEquals(raoRequest.getInstant(), importedRaoRequest.getInstant());
        assertEquals(raoRequest.getNetworkFileUrl(), importedRaoRequest.getNetworkFileUrl());
        assertEquals(raoRequest.getCracFileUrl(), importedRaoRequest.getCracFileUrl());
        assertEquals(raoRequest.getRefprogFileUrl(), importedRaoRequest.getRefprogFileUrl());
        assertEquals(raoRequest.getRealGlskFileUrl(), importedRaoRequest.getRealGlskFileUrl());
        assertEquals(raoRequest.getRaoParametersFileUrl(), importedRaoRequest.getRaoParametersFileUrl());
        assertEquals(raoRequest.getResultsDestination(), importedRaoRequest.getResultsDestination());
        assertEquals(raoRequest.getTargetEndInstant(), importedRaoRequest.getTargetEndInstant());
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
        assertEquals("id", raoRequest.getId());
        assertEquals("instant", raoRequest.getInstant().get());
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertEquals("cracFileUrl", raoRequest.getCracFileUrl());
        assertEquals("raoParametersFileUrl", raoRequest.getRaoParametersFileUrl());
        assertEquals(Optional.empty(), raoRequest.getRefprogFileUrl());
        assertEquals(Optional.empty(), raoRequest.getRealGlskFileUrl());
        assertEquals(Optional.empty(), raoRequest.getResultsDestination());
        assertEquals(Optional.empty(), raoRequest.getTargetEndInstant());
        assertEquals(Optional.empty(), raoRequest.getEventPrefix());
    }

    @Test
    void checkInternalExceptionJsonConversion() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] errorBytes = getClass().getResourceAsStream("/ErrorMessage.json").readAllBytes();
        AbstractRaoRunnerException exception = new RaoRunnerException("Something bad happened");
        String expectedMessage = new String(errorBytes);
        assertEquals(expectedMessage, new String(jsonConverter.toJsonMessage(exception)));
    }

    @Test
    void checkRaoResponseJsonConversion() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoResponseMessage.json").readAllBytes();
        RaoResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, RaoResponse.class);
        assertEquals("id", raoResponse.getId());
        assertEquals("instant", raoResponse.getInstant().get());
        assertEquals("networkWithPraFileUrl", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("cracFileUrl", raoResponse.getCracFileUrl());
        assertEquals("raoResultFileUrl", raoResponse.getRaoResultFileUrl());
        assertEquals(Instant.ofEpochSecond(1637052884, 944727000), raoResponse.getComputationStartInstant());
        assertEquals(Instant.ofEpochSecond(1647057884, 934927000), raoResponse.getComputationEndInstant());
    }

    @Test
    void checkRaoResponseJsonConversionWhithNullInstant() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoResponseMessageNullInstant.json").readAllBytes();
        RaoResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, RaoResponse.class);
        assertEquals("id", raoResponse.getId());
        assertEquals(Optional.empty(), raoResponse.getInstant());
        assertEquals("networkWithPraFileUrl", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("cracFileUrl", raoResponse.getCracFileUrl());
        assertEquals("raoResultFileUrl", raoResponse.getRaoResultFileUrl());
        assertNull(raoResponse.getComputationStartInstant());
        assertNull(raoResponse.getComputationEndInstant());
    }
}
