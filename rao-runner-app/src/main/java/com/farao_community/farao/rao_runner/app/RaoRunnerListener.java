/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.app.configuration.AmqpConfiguration;
import com.farao_community.farao.rao_runner.app.configuration.RaoRunnerEventsLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Component;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Component
public class RaoRunnerListener  implements MessageListener {

    private final Logger raoRunnerEventsLogger;
    private static final String APPLICATION_ID = "rao-runner-server";
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int PRIORITY = 1;

    private final JsonApiConverter jsonApiConverter;
    private final RaoRunnerService raoRunnerServer;
    private final AmqpTemplate amqpTemplate;
    private final AmqpConfiguration amqpConfiguration;
    private final RaoRunnerEventsLogging raoRunnerEventsLogging;

    public RaoRunnerListener(Logger raoRunnerEventsLogger, RaoRunnerService raoRunnerServer, AmqpTemplate amqpTemplate, AmqpConfiguration amqpConfiguration, RaoRunnerEventsLogging raoRunnerEventsLogging) {
        this.raoRunnerEventsLogger = raoRunnerEventsLogger;
        this.raoRunnerEventsLogging = raoRunnerEventsLogging;
        this.jsonApiConverter = new JsonApiConverter();
        this.raoRunnerServer = raoRunnerServer;
        this.amqpTemplate = amqpTemplate;
        this.amqpConfiguration = amqpConfiguration;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();
        try {
            RaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), RaoRequest.class);

            String processId = raoRequest.getId().substring(0, raoRequest.getId().lastIndexOf("-20210901"));
            LoggerFactory.getLogger("TEST").info("processId {}", processId);
            addMetaDataToLogsModelContext(processId, correlationId, message.getMessageProperties().getAppId());
            LoggerFactory.getLogger("TEST").info("RAO request received: {}", raoRequest);
            RaoResponse raoResponse = raoRunnerServer.runRao(raoRequest);
            LoggerFactory.getLogger("TEST").info("processId {}", processId);
            sendRaoResponse(raoResponse, replyTo, correlationId);
            LoggerFactory.getLogger("TEST").info("processId {}", processId);

        } catch (RaoRunnerException e) {
            LoggerFactory.getLogger("TEST").info("RaoRunnerException", e);
            sendErrorResponse(e, replyTo, correlationId);
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("TEST").info("RuntimeException", e);

            RaoRunnerException wrappingException = new RaoRunnerException("Unhandled exception: " + e.getMessage(), e);
            sendErrorResponse(wrappingException, replyTo, correlationId);
        }
    }

    public void addMetaDataToLogsModelContext(String gridcapaTaskId, String raoRequestId, String clientAppId) {
        MDC.put("gridcapa-task-id", gridcapaTaskId);
        MDC.put("rao-request-id", raoRequestId);
        MDC.put("client-app-id", clientAppId);
    }

    private void sendRaoResponse(RaoResponse raoResponse, String replyTo, String correlationId) {
        Message responseMessage = createMessageResponse(raoResponse, correlationId);
        if (replyTo != null) {
            amqpTemplate.send(replyTo, responseMessage);
        } else {
            amqpTemplate.send(amqpConfiguration.raoResponseExchange().getName(), "", responseMessage);
        }
        raoRunnerEventsLogger.info("RAO response sent: Response message: {}", responseMessage);
    }

    private void sendErrorResponse(RaoRunnerException exception, String replyTo, String correlationId) {
        Message errorMessage = createErrorResponse(exception, correlationId);
        if (replyTo != null) {
            amqpTemplate.send(replyTo, errorMessage);
        } else {
            amqpTemplate.send(amqpConfiguration.raoResponseExchange().getName(), "", errorMessage);
        }
        raoRunnerEventsLogger.warn("RAO response sent: Response message: {}", errorMessage);
    }

    private Message createMessageResponse(RaoResponse raoResponse, String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(raoResponse))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private Message createErrorResponse(RaoRunnerException exception, String correlationId) {
        return MessageBuilder.withBody(exceptionToJsonMessage(exception))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private byte[] exceptionToJsonMessage(RaoRunnerException e) {
        return jsonApiConverter.toJsonMessage(e);
    }

    private MessageProperties buildMessageResponseProperties(String correlationId) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(APPLICATION_ID)
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpConfiguration.raoResponseExpiration())
                .setPriority(PRIORITY)
                .build();
    }
}
