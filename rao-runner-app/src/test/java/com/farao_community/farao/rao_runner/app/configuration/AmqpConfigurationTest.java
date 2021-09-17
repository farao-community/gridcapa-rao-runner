/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class AmqpConfigurationTest {

    @Autowired
    private AmqpConfiguration amqpConfiguration;

    @Autowired
    private Queue raoRequestQueue;

    @Autowired
    private FanoutExchange raoResponseExchange;

    @Test
    void checkAmqpMessageConfiguration() {
        assertNotNull(amqpConfiguration);
        assertNotNull(raoRequestQueue);
        assertEquals("raoi-request-queue", raoRequestQueue.getName());
        assertNotNull(raoResponseExchange);
        assertEquals("raoi-response", raoResponseExchange.getName());
        assertEquals("60000", amqpConfiguration.raoResponseExpiration());
    }
}
