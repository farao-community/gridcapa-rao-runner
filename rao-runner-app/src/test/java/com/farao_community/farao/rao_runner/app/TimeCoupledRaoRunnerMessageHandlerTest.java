/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoSuccessResponse;
import com.powsybl.openrao.commons.OpenRaoException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TimeCoupledRaoRunnerMessageHandlerTest {

    @Autowired
    TimeCoupledRaoRunnerMessageHandler raoRunnerMessageHandler;

    @MockitoBean
    RestTemplateBuilder restTemplateBuilder;

    @MockitoBean
    RabbitTemplate amqpTemplate;

    @MockitoBean
    TimeCoupledRaoRunnerService raoRunnerService;

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"prefix"})
    void mdcMetadataPropagationTest(final String prefixValue) {
        final Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        raoRunnerMessageHandler.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.ofNullable(prefixValue));
        logger.info("message");

        final Map<String, String> mdcPropertyMap = listAppender.list.getFirst().getMDCPropertyMap();
        Assertions.assertThat(mdcPropertyMap)
            .hasSize(prefixValue == null ? 3 : 4)
            .containsEntry("gridcapaTaskId", "process-id")
            .containsEntry("computationId", "request-id")
            .containsEntry("clientAppId", "client-id");
        final String eventPrefix = mdcPropertyMap.get("eventPrefix");
        Assertions.assertThat(eventPrefix).isEqualTo(prefixValue);
    }

    @Test
    void pendingInterruptionTest() throws IOException {
        final byte[] request = getClass().getResourceAsStream("/timeCoupledRaoRequestMessage.json").readAllBytes();
        final MessageProperties properties = new MessageProperties();
        properties.setReplyTo("ReplyTo");
        properties.setCorrelationId("CorrelationId");
        final Message message = new Message(request, properties);

        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForEntity(anyString(), any(Class.class))).thenReturn(ResponseEntity.ok(Boolean.TRUE));

        raoRunnerMessageHandler.handleMessage(message);

        Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.eq("ReplyTo"), Mockito.any(Message.class));
        Mockito.verify(raoRunnerService, Mockito.never()).runRao(Mockito.any(TimeCoupledRaoRequest.class));
    }

    @Test
    void onMessageRaoHasErrorTest() {
        final TimeCoupledRaoRequest request = new TimeCoupledRaoRequest.RaoRequestBuilder()
                .withId("id")
                .withRunId("runId")
                .withEventPrefix("prefix")
                .build();
        final MessageProperties properties = new MessageProperties();
        properties.setReplyTo("replyToMe");
        properties.setCorrelationId("correlationId");
        final Message message = new Message(jsonApiConverter.toJsonMessage(request), properties);

        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForEntity(anyString(), any(Class.class))).thenReturn(ResponseEntity.ok(Boolean.FALSE));
        when(raoRunnerService.runRao(any())).thenThrow(new OpenRaoException("Hey I just met you"));

        raoRunnerMessageHandler.handleMessage(message);

        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate, times(1)).send(eq("replyToMe"), messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue()).isNotNull();
        final RaoFailureResponse response = jsonApiConverter.fromJsonMessage(messageCaptor.getValue().getBody(), RaoFailureResponse.class);
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    void onMessageRaoSuccessTest() {
        final TimeCoupledRaoRequest request = new TimeCoupledRaoRequest.RaoRequestBuilder()
                .withId("id")
                .withRunId("runId")
                .withEventPrefix("prefix")
                .build();
        final MessageProperties properties = new MessageProperties();
        properties.setReplyTo("replyToMe");
        properties.setCorrelationId("correlationId");
        final Message message = new Message(jsonApiConverter.toJsonMessage(request), properties);

        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForEntity(anyString(), any(Class.class))).thenReturn(ResponseEntity.ok(Boolean.FALSE));

        final TimeCoupledRaoSuccessResponse raoSuccessResponse = new TimeCoupledRaoSuccessResponse.Builder().withId("testId").build();
        when(raoRunnerService.runRao(any())).thenReturn(raoSuccessResponse);

        raoRunnerMessageHandler.handleMessage(message);

        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate, times(1)).send(eq("replyToMe"), messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue()).isNotNull();
        final TimeCoupledRaoSuccessResponse response = jsonApiConverter.fromJsonMessage(messageCaptor.getValue().getBody(), TimeCoupledRaoSuccessResponse.class);
        Assertions.assertThat(response).isNotNull();
    }
}
