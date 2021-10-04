/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoRequestTest {

    @Test
    void checkRaoRequestNormalUsage() {
        RaoRequest raoRequest = new RaoRequest("id", "instant", "networkFileUrl", "cracFileUrl", "refprogFileUrl", "glskFileUrl", "raoParametersFileUrl", "resultsDestination");
        assertNotNull(raoRequest);
        assertEquals("instant", raoRequest.getInstant().get());
    }

    @Test
    void checkRaoRequestWithEmptyOptionals() {
        RaoRequest raoRequest = new RaoRequest("id", "networkFileUrl", "cracFileUrl", "raoParametersFileUrl");
        assertNotNull(raoRequest);
        assertEquals("networkFileUrl", raoRequest.getNetworkFileUrl());
    }

}
