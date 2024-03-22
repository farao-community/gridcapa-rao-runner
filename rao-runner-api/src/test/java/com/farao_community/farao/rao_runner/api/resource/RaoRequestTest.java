/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoRequestTest {

    @Test
    void checkRaoRequestNormalUsage() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .withRefprogFileUrl("refprogFileUrl")
                .withRealGlskFileUrl("glskFileUrl")
                .withRaoParametersFileUrl("raoParametersFileUrl")
                .withResultsDestination("resultsDestination")
                .build();
        assertNotNull(raoRequest);
        assertEquals("instant", raoRequest.getInstant().get());
        assertTrue(raoRequest.getTargetEndInstant().isEmpty());
    }

    @Test
    void checkRaoRequestWithEmptyOptionals() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .build();
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
        assertTrue(raoRequest.getResultsDestination().isEmpty());
    }

    @Test
    void checkRaoRequestWithEmptyOptionalsAndResultDestination() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .withRaoParametersFileUrl("raoParametersFileUrl")
                .withResultsDestination("resultDestination")
                .build();
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertEquals("resultDestination", raoRequest.getResultsDestination().get());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
    }

    @Test
    void checkRaoRequestWithEventPrefix() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .withRefprogFileUrl("refprogFileUrl")
                .withRealGlskFileUrl("glskFileUrl")
                .withRaoParametersFileUrl("raoParametersFileUrl")
                .withResultsDestination("resultsDestination")
                .withEventPrefix("eventPrefix")
                .build();
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertEquals("eventPrefix", raoRequest.getEventPrefix().get());
        assertTrue(raoRequest.getTargetEndInstant().isEmpty());
    }

    @Test
    void checkRaoRequestWithoutEventPrefix() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("instant")
                .withNetworkFileUrl("networkFileUrl")
                .withCracFileUrl("cracFileUrl")
                .withRefprogFileUrl("refprogFileUrl")
                .withRealGlskFileUrl("glskFileUrl")
                .withRaoParametersFileUrl("raoParametersFileUrl")
                .withResultsDestination("resultsDestination")
                .build();
        assertNotNull(raoRequest);
        assertEquals("instant", raoRequest.getInstant().get());
        assertTrue(raoRequest.getTargetEndInstant().isEmpty());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
    }
}
