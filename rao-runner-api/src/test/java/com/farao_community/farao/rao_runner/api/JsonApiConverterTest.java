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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals("raoParametersFileUrl", raoRequest.getRaoParametersFileUrl().get());
        assertEquals("resultsDestination", raoRequest.getResultsDestination().get());
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
        assertEquals("refprogFileUrl", raoRequest.getRefprogFileUrl().get());
        assertEquals("realGlskFileUrl", raoRequest.getRealGlskFileUrl().get());
        assertEquals(Optional.empty(), raoRequest.getRaoParametersFileUrl());
        assertEquals(Optional.empty(), raoRequest.getResultsDestination());
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
        assertEquals("instant", raoResponse.getInstant());
        assertEquals("networkWithPraFileUrl", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("cracFileUrl", raoResponse.getCracFileUrl());
        assertEquals("raoResultFileUrl", raoResponse.getRaoResultFileUrl());
    }

    @Test
    void checkRaoResponseJsonConversionWhithNullInstant() throws IOException {
        JsonApiConverter jsonConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/raoResponseMessageNullInstant.json").readAllBytes();
        RaoResponse raoResponse = jsonConverter.fromJsonMessage(requestBytes, RaoResponse.class);
        assertEquals("id", raoResponse.getId());
        assertNull(raoResponse.getInstant());
        assertEquals("networkWithPraFileUrl", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("cracFileUrl", raoResponse.getCracFileUrl());
        assertEquals("raoResultFileUrl", raoResponse.getRaoResultFileUrl());
    }
}
