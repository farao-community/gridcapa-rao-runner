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
        RaoRequest raoRequest = new RaoRequest("id", "instant", "networkFileUrl", "cracFileUrl", "refprogFileUrl", "glskFileUrl", "raoParametersFileUrl", "resultsDestination", null, null);
        assertNotNull(raoRequest);
        assertEquals("instant", raoRequest.getInstant().get());
        assertTrue(raoRequest.getTargetEndInstant().isEmpty());
    }

    @Test
    void checkRaoRequestWithEmptyOptionals() {
        RaoRequest raoRequest = new RaoRequest("id", "networkFileUrl", "cracFileUrl", "raoParametersFileUrl");
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
        assertTrue(raoRequest.getResultsDestination().isEmpty());
    }

    @Test
    void checkRaoRequestWithEmptyOptionalsAndResultDestination() {
        RaoRequest raoRequest = new RaoRequest("id", "networkFileUrl", "cracFileUrl", "raoParametersFileUrl", "resultDestination");
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertEquals("resultDestination", raoRequest.getResultsDestination().get());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
    }

    @Test
    void checkRaoRequestWithEventPrefix() {
        RaoRequest raoRequest = new RaoRequest("id", "networkFileUrl", "cracFileUrl", "raoParametersFileUrl", "resultsDestination", "eventPrefix");
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
        assertEquals("eventPrefix", raoRequest.getEventPrefix().get());
        assertTrue(raoRequest.getTargetEndInstant().isEmpty());
    }

    @Test
    void checkRaoRequestWithoutEventPrefix() {
        RaoRequest raoRequest = new RaoRequest("id", "instant", "networkFileUrl", "cracFileUrl", "refprogFileUrl", "glskFileUrl", "raoParametersFileUrl", "resultsDestination", null);
        assertNotNull(raoRequest);
        assertEquals("instant", raoRequest.getInstant().get());
        assertTrue(raoRequest.getTargetEndInstant().isEmpty());
        assertTrue(raoRequest.getEventPrefix().isEmpty());
    }
}
