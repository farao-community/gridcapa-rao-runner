/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerListenerTest {

    @Autowired
    public RaoRunnerListener raoRunnerListener;

    @MockBean
    public RaoRunnerService raoRunnerServer;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @TestConfiguration
    static class AmqpTestConfiguration {
        @Bean
        @Primary
        public AmqpTemplate amqpTemplate() {
            return Mockito.mock(AmqpTemplate.class);
        }
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(amqpTemplate, raoRunnerServer);
    }

    @Test
    void checkThatCorrectMessageIsHandledCorrectly() throws IOException {
        File resource = new ClassPathResource("raoRequestMessage.json").getFile();
        String inputMessage = new String(Files.readAllBytes(resource.toPath()));
        Message message = MessageBuilder.withBody(inputMessage.getBytes()).build();
        raoRunnerListener.onMessage(message);
        Mockito.verify(raoRunnerServer, Mockito.times(1)).runRao(Mockito.any(RaoRequest.class));
    }

    @Test
    void checkInvalidMessageReturnsError() throws IOException {
        File resource = new ClassPathResource("fakeMessage.json").getFile();
        String inputMessage = new String(Files.readAllBytes(resource.toPath()));
        Message message = MessageBuilder.withBody(inputMessage.getBytes()).build();
        raoRunnerListener.onMessage(message);
        Mockito.verify(raoRunnerServer, Mockito.times(0)).runRao(Mockito.any(RaoRequest.class));
    }
}
