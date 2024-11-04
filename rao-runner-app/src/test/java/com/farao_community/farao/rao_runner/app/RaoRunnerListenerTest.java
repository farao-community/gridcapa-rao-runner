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
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.openrao.commons.OpenRaoException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerListenerTest {

    @Autowired
    RaoRunnerListener raoRunnerListener;

    @MockBean
    RestTemplateBuilder restTemplateBuilder;

    @MockBean
    RabbitTemplate amqpTemplate;

    @MockBean
    RaoRunnerService raoRunnerService;

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void checkThatMdcMetadataIsPropagatedCorrectly() {
        Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        raoRunnerListener.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.of("prefix"));
        logger.info("message");
        assertEquals(4, listAppender.list.get(0).getMDCPropertyMap().size());
        assertEquals("process-id", listAppender.list.get(0).getMDCPropertyMap().get("gridcapaTaskId"));
        assertEquals("request-id", listAppender.list.get(0).getMDCPropertyMap().get("computationId"));
        assertEquals("client-id", listAppender.list.get(0).getMDCPropertyMap().get("clientAppId"));
        assertEquals("prefix", listAppender.list.get(0).getMDCPropertyMap().get("eventPrefix"));
    }

    @Test
    void checkThatMdcMetadataIsPropagatedCorrectlyWithoutPrefix() {
        Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        raoRunnerListener.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.empty());
        logger.info("message");
        assertEquals(3, listAppender.list.get(0).getMDCPropertyMap().size());
        assertEquals("process-id", listAppender.list.get(0).getMDCPropertyMap().get("gridcapaTaskId"));
        assertEquals("request-id", listAppender.list.get(0).getMDCPropertyMap().get("computationId"));
        assertEquals("client-id", listAppender.list.get(0).getMDCPropertyMap().get("clientAppId"));
        assertNull(listAppender.list.get(0).getMDCPropertyMap().get("eventPrefix"));
    }

    @Test
    void checkThatPendingInterruptWorks() throws IOException {
        Message message = Mockito.mock(Message.class);
        byte[] requestBytes = getClass().getResourceAsStream("/raoRequestMessage.json").readAllBytes();
        when(message.getBody()).thenReturn(requestBytes);
        MessageProperties messageProperties = Mockito.mock(MessageProperties.class);
        RaoRequest raoRequest = Mockito.mock(RaoRequest.class);

        Mockito.when(message.getMessageProperties()).thenReturn(messageProperties);
        Mockito.when(messageProperties.getReplyTo()).thenReturn("ReplyTo");
        Mockito.when(messageProperties.getCorrelationId()).thenReturn("CorrelationId");

        Mockito.when(raoRequest.getRunId()).thenReturn("MyRunId");

        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        ResponseEntity<Boolean> responseEntity = mock(ResponseEntity.class);
        when(restTemplate.getForEntity(anyString(), any(Class.class))).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn(Boolean.TRUE);

        raoRunnerListener.onMessage(message);

        Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.eq("ReplyTo"), Mockito.any(Message.class));
        Mockito.verify(raoRunnerService, Mockito.never()).runRao(Mockito.any(RaoRequest.class));
    }

    @Test
    void onMessageRaoHasError() {
        final RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withRunId("runId")
                .withEventPrefix("prefix")
                .build();
        final MessageProperties properties = new MessageProperties();
        properties.setReplyTo("replyToMe");
        properties.setCorrelationId("correlationId");
        final Message message = MessageBuilder
                .withBody(jsonApiConverter.toJsonMessage(raoRequest))
                .andProperties(properties)
                .build();

        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        ResponseEntity<Boolean> responseEntity = mock(ResponseEntity.class);
        when(restTemplate.getForEntity(anyString(), any(Class.class))).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn(Boolean.FALSE);

        when(raoRunnerService.runRao(any())).thenThrow(new OpenRaoException("Hey I just met you"));

        raoRunnerListener.onMessage(message);

        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate, times(1)).send(eq("replyToMe"), messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue()).isNotNull();
        final RaoFailureResponse response = jsonApiConverter.fromJsonMessage(messageCaptor.getValue().getBody(), RaoFailureResponse.class);
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    void onMessageRaoSuccess() {
        final RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withRunId("runId")
                .withEventPrefix("prefix")
                .build();
        final MessageProperties properties = new MessageProperties();
        properties.setReplyTo("replyToMe");
        properties.setCorrelationId("correlationId");
        final Message message = MessageBuilder
                .withBody(jsonApiConverter.toJsonMessage(raoRequest))
                .andProperties(properties)
                .build();

        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final ResponseEntity<Boolean> responseEntity = mock(ResponseEntity.class);
        when(restTemplate.getForEntity(anyString(), any(Class.class))).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn(Boolean.FALSE);

        final RaoSuccessResponse raoSuccessResponse = new RaoSuccessResponse.Builder().withId("testId").build();
        when(raoRunnerService.runRao(any())).thenReturn(raoSuccessResponse);

        raoRunnerListener.onMessage(message);

        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate, times(1)).send(eq("replyToMe"), messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue()).isNotNull();
        final RaoSuccessResponse response = jsonApiConverter.fromJsonMessage(messageCaptor.getValue().getBody(), RaoSuccessResponse.class);
        Assertions.assertThat(response).isNotNull();
    }
}
