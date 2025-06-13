/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoRunnerClientAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void createContext() {
        context = new AnnotationConfigApplicationContext();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void registersRaoRunnerClient() {
        context.registerBean("amqpTemplate", AmqpTemplate.class, () -> Mockito.mock(AmqpTemplate.class));
        context.register(RaoRunnerClientAutoConfiguration.class);
        context.refresh();
        RaoRunnerClient raoRunnerClient = context.getBean(RaoRunnerClient.class);
        assertNotNull(raoRunnerClient);
    }

    @Test
    void registersAsynchRaoRunnerClient() {
        context.registerBean("amqpTemplate", AmqpTemplate.class, () -> Mockito.mock(AmqpTemplate.class));
        context.registerBean("asynchAmqpTemplate", AsyncAmqpTemplate.class, () -> Mockito.mock(AsyncAmqpTemplate.class));
        context.register(RaoRunnerClientAutoConfiguration.class);
        context.refresh();
        AsynchronousRaoRunnerClient raoRunnerClient = context.getBean(AsynchronousRaoRunnerClient.class);
        assertNotNull(raoRunnerClient);
    }

}
