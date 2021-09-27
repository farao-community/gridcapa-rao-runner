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
class RaoResponseTest {

    @Test
    void checkRaoRequestNormalUsage() {
        RaoResponse raoResponse = new RaoResponse("id", "instant", "networkWithPraFileUrl", "jsonCracFileUrl", "raoResultFileUrl");
        assertNotNull(raoResponse);
        assertEquals("instant", raoResponse.getInstant().get());
    }
}
