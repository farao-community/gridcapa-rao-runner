/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class UrlConfigurationTest {

    @Autowired
    public UrlConfiguration urlConfiguration;

    @Test
    void checkUrlWhiteListIsRetrievedCorrectly() {
        Assertions.assertEquals(2, urlConfiguration.getWhitelist().size());
        Assertions.assertEquals("http://localhost:9000", urlConfiguration.getWhitelist().get(0));
        Assertions.assertEquals("file:/", urlConfiguration.getWhitelist().get(1));
    }

    @Test
    void checkInterruptUrlRetrievedCorrectly() {
        Assertions.assertEquals("http://testUrl/interrupted/", urlConfiguration.getInterruptServerUrl());
    }
}
