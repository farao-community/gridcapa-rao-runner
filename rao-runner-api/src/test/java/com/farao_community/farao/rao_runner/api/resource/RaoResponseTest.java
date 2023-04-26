/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoResponseTest {

    @Test
    void checkRaoRequestNormalUsage() {
        RaoResponse raoResponse = new RaoResponse("id", "instant", "networkWithPraFileUrl", "jsonCracFileUrl", "raoResultFileUrl", Instant.ofEpochSecond(3600), Instant.ofEpochSecond(7200), true);
        assertNotNull(raoResponse);
        assertEquals("id", raoResponse.getId());
        assertEquals("instant", raoResponse.getInstant().get());
        assertEquals("networkWithPraFileUrl", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("jsonCracFileUrl", raoResponse.getCracFileUrl());
        assertEquals("raoResultFileUrl", raoResponse.getRaoResultFileUrl());
        assertEquals(Instant.ofEpochSecond(3600), raoResponse.getComputationStartInstant());
        assertEquals(Instant.ofEpochSecond(7200), raoResponse.getComputationEndInstant());
        assertTrue(raoResponse.isInterrupted());
    }
}
