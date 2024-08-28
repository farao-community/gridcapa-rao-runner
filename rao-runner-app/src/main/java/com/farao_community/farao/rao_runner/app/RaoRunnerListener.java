/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.api.resource.ThreadLauncherResult;
import com.farao_community.farao.rao_runner.app.configuration.AmqpConfiguration;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Component
public class RaoRunnerListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoRunnerListener.class);
    private static final String APPLICATION_ID = "rao-runner-server";
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int PRIORITY = 1;

    private final JsonApiConverter jsonApiConverter;
    private final RaoRunnerService raoRunnerService;
    private final AmqpTemplate amqpTemplate;
    private final AmqpConfiguration amqpConfiguration;
    private final Logger businessLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final UrlConfiguration urlConfiguration;

    public RaoRunnerListener(RaoRunnerService raoRunnerService, AmqpTemplate amqpTemplate, AmqpConfiguration amqpConfiguration, Logger businessLogger, RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        this.businessLogger = businessLogger;
        this.jsonApiConverter = new JsonApiConverter();
        this.raoRunnerService = raoRunnerService;
        this.amqpTemplate = amqpTemplate;
        this.amqpConfiguration = amqpConfiguration;
        this.restTemplateBuilder = restTemplateBuilder;
        this.urlConfiguration = urlConfiguration;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String brokerCorrelationId = message.getMessageProperties().getCorrelationId();

        try {
            RaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), RaoRequest.class);
            LOGGER.info("RAO request received: {}", raoRequest);
            if (checkIsInterrupted(raoRequest)) {
                sendRaoResponseInterrupted(raoRequest, replyTo, brokerCorrelationId);
                return;
            }
            addMetaDataToLogsModelContext(raoRequest.getId(), brokerCorrelationId, message.getMessageProperties().getAppId(), raoRequest.getEventPrefix());
            GenericThreadLauncher<RaoRunnerService, RaoResponse> launcher = new GenericThreadLauncher<>(
                raoRunnerService,
                raoRequest.getId(),
                MDC.getCopyOfContextMap(),
                raoRequest
            );

            businessLogger.info("Starting the RAO computation");
            launcher.start();

            ThreadLauncherResult<RaoResponse> raoThreadResult = launcher.getResult();
            if (raoThreadResult.hasError() && raoThreadResult.getException() != null) {
                Exception exception = raoThreadResult.getException();
                if (exception instanceof InvocationTargetException ite
                        && ite.getCause() instanceof RaoRunnerException rre) {
                    throw rre;
                } else {
                    throw exception;
                }
            }

            Optional<RaoResponse> raoResponseOpt = raoThreadResult.getResult();
            if (raoResponseOpt.isPresent() && !raoThreadResult.hasError()) {
                businessLogger.info("RAO computation is finished");
                RaoResponse raoResponse = raoResponseOpt.get();
                LOGGER.info("RAO response sent: {}", raoResponse);
                sendRaoResponse(raoResponse, replyTo, brokerCorrelationId);
            } else {
                sendRaoResponseInterrupted(raoRequest, replyTo, brokerCorrelationId);
            }
            System.gc();
        } catch (RaoRunnerException e) {
            sendErrorResponse(e, replyTo, brokerCorrelationId);
        } catch (Exception e) {
            RaoRunnerException wrappingException = new RaoRunnerException("Unhandled exception: " + e.getMessage(), e);
            sendErrorResponse(wrappingException, replyTo, brokerCorrelationId);
        }
    }

    private void sendRaoResponseInterrupted(RaoRequest raoRequest, String replyTo, String brokerCorrelationId) {
        businessLogger.warn("RAO computation has been interrupted");
        LOGGER.info("RAO run has been interrupted");
        sendRaoResponse(new RaoResponse.RaoResponseBuilder().withId(raoRequest.getId()).withInterrupted(true).build(),
                replyTo,
                brokerCorrelationId);
    }

    void addMetaDataToLogsModelContext(String gridcapaTaskId, String computationId, String clientAppId, Optional<String> optPrefix) {
        MDC.put("gridcapaTaskId", gridcapaTaskId);
        MDC.put("computationId", computationId);
        MDC.put("clientAppId", clientAppId);
        if (optPrefix.isPresent()) {
            MDC.put("eventPrefix", optPrefix.get());
        } else {
            MDC.remove("eventPrefix");
        }
    }

    private void sendRaoResponse(RaoResponse raoResponse, String replyTo, String correlationId) {
        Message responseMessage = createMessageResponse(raoResponse, correlationId);
        if (replyTo != null) {
            amqpTemplate.send(replyTo, responseMessage);
        } else {
            amqpTemplate.send(amqpConfiguration.raoResponseExchange().getName(), "", responseMessage);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Response message: {}", new String(responseMessage.getBody()));
        }
    }

    private void sendErrorResponse(RaoRunnerException exception, String replyTo, String correlationId) {
        LOGGER.error("Exception occurred while running RAO", exception);
        Message errorMessage = createErrorResponse(exception, correlationId);
        if (replyTo != null) {
            amqpTemplate.send(replyTo, errorMessage);
        } else {
            amqpTemplate.send(amqpConfiguration.raoResponseExchange().getName(), "", errorMessage);
        }
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

    private boolean checkIsInterrupted(RaoRequest raoRequest) {
        ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(getInterruptedUrl(raoRequest.getRunId()), Boolean.class);
        return responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody();
    }

    private String getInterruptedUrl(String runId) {
        return urlConfiguration.getInterruptServerUrl() + runId;
    }
}
