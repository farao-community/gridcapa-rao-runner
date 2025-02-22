/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoSuccessResponseTest {

    @Test
    void checkRaoRequestNormalUsage() {
        RaoSuccessResponse raoResponse = new RaoSuccessResponse.Builder()
                .withId("id")
                .withInstant("instant")
                .withNetworkWithPraFileUrl("networkWithPraFileUrl")
                .withCracFileUrl("jsonCracFileUrl")
                .withRaoResultFileUrl("raoResultFileUrl")
                .withComputationStartInstant(Instant.ofEpochSecond(3600))
                .withComputationEndInstant(Instant.ofEpochSecond(7200))
                .withInterrupted(true)
                .build();
        assertNotNull(raoResponse);
        assertEquals("instant", raoResponse.getInstant().get());
        assertEquals("networkWithPraFileUrl", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("jsonCracFileUrl", raoResponse.getCracFileUrl());
        assertEquals("raoResultFileUrl", raoResponse.getRaoResultFileUrl());
        assertEquals(Instant.ofEpochSecond(3600), raoResponse.getComputationStartInstant());
        assertEquals(Instant.ofEpochSecond(7200), raoResponse.getComputationEndInstant());
        assertTrue(raoResponse.isInterrupted());
    }
}
