/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class GenericRaoRunnerListenerTest {
    private GenericRaoRunnerListener genericListener;
    private RaoRunnerMessageHandler stantardRaoRunnerMessageHandler;
    private TimeCoupledRaoRunnerMessageHandler timeCoupledRaoRunnerMessageHandler;

    @BeforeEach
    void setUp() {
        stantardRaoRunnerMessageHandler = mock(RaoRunnerMessageHandler.class);
        timeCoupledRaoRunnerMessageHandler = mock(TimeCoupledRaoRunnerMessageHandler.class);
        genericListener = new GenericRaoRunnerListener(
            stantardRaoRunnerMessageHandler,
            timeCoupledRaoRunnerMessageHandler
        );
    }

    @Test
    void genericLauncherTriggersTimeCoupledHandlerTest() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setReceivedRoutingKey("TIME-COUPLED");
        final Message message = new Message("Test".getBytes(), messageProperties);

        genericListener.onMessage(message);

        Mockito.verify(stantardRaoRunnerMessageHandler, times(0)).handleMessage(message);
        Mockito.verify(timeCoupledRaoRunnerMessageHandler, times(1)).handleMessage(message);
    }

    @Test
    void genericLauncherTriggersStandardHandlerTest() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setReceivedRoutingKey("");
        final Message message = new Message("Test".getBytes(), messageProperties);

        genericListener.onMessage(message);

        Mockito.verify(stantardRaoRunnerMessageHandler, times(1)).handleMessage(message);
        Mockito.verify(timeCoupledRaoRunnerMessageHandler, times(0)).handleMessage(message);
    }
}
