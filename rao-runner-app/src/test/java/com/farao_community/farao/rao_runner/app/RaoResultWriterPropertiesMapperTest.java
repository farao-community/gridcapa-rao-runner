/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.commons.Unit;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.rao_runner.app.RaoResultWriterPropertiesMapper.generateJsonProperties;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
class RaoResultWriterPropertiesMapperTest {

    @Test
    void testJsonProperties() {
        assertEquals("true", generateJsonProperties(Unit.AMPERE).getProperty("rao-result.export.json.flows-in-amperes"));
        assertNull(generateJsonProperties(Unit.AMPERE).getProperty("rao-result.export.json.flows-in-megawatts"));
        assertEquals("true", generateJsonProperties(Unit.MEGAWATT).getProperty("rao-result.export.json.flows-in-megawatts"));
    }
}
