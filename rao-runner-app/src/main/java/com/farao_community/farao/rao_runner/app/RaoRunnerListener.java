/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.app.configuration.AmqpConfiguration;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${rao-runner.with-interruption-server}")
    private boolean interruptionServerIsActivated;

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
        final String replyTo = message.getMessageProperties().getReplyTo();
        final String brokerCorrelationId = message.getMessageProperties().getCorrelationId();

        try {
            final RaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), RaoRequest.class);
            LOGGER.info("RAO request received: {}", raoRequest);
            if (interruptionServerIsActivated && checkIsInterrupted(raoRequest)) {
                sendRaoInterruptedResponse(raoRequest, replyTo, brokerCorrelationId);
                return;
            }
            addMetaDataToLogsModelContext(raoRequest.getId(), brokerCorrelationId, message.getMessageProperties().getAppId(), raoRequest.getEventPrefix());
            final GenericThreadLauncher<RaoRunnerService, AbstractRaoResponse> launcher = new GenericThreadLauncher<>(
                raoRunnerService,
                raoRequest.getId(),
                MDC.getCopyOfContextMap(),
                raoRequest
            );

            businessLogger.info("Starting the RAO computation");
            launcher.start();

            final ThreadLauncherResult<AbstractRaoResponse> raoThreadResult = launcher.getResult();
            if (raoThreadResult.hasError()) {
                final Exception exception = raoThreadResult.exception();
                if (exception instanceof InvocationTargetException ite) {
                    throw (Exception) ite.getCause();
                } else {
                    throw exception;
                }
            } else if (raoThreadResult.isInterrupted()) {
                sendRaoInterruptedResponse(raoRequest, replyTo, brokerCorrelationId);
            } else {
                final AbstractRaoResponse raoResponse = raoThreadResult.result();
                businessLogger.info("RAO computation is finished");
                LOGGER.info("RAO response sent: {}", raoResponse);
                sendRaoResponse(raoResponse, replyTo, brokerCorrelationId);
            }
            System.gc();
        } catch (RaoRunnerException e) {
            sendRaoFailedResponse(e, replyTo, brokerCorrelationId);
        } catch (Exception e) {
            final RaoRunnerException wrappingException = new RaoRunnerException("Unhandled exception: " + e.getMessage(), e);
            sendRaoFailedResponse(wrappingException, replyTo, brokerCorrelationId);
        }
    }

    private boolean checkIsInterrupted(final RaoRequest raoRequest) {
        final String requestUrl = urlConfiguration.getInterruptServerUrl() + raoRequest.getRunId();
        final ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, Boolean.class);
        return responseEntity.getBody() != null
                && responseEntity.getStatusCode() == HttpStatus.OK
                && responseEntity.getBody();
    }

    void addMetaDataToLogsModelContext(final String gridcapaTaskId, final String computationId, final String clientAppId, final Optional<String> optPrefix) {
        MDC.put("gridcapaTaskId", gridcapaTaskId);
        MDC.put("computationId", computationId);
        MDC.put("clientAppId", clientAppId);
        if (optPrefix.isPresent()) {
            MDC.put("eventPrefix", optPrefix.get());
        } else {
            MDC.remove("eventPrefix");
        }
    }

    private void sendRaoInterruptedResponse(final RaoRequest raoRequest, final String replyTo, final String brokerCorrelationId) {
        businessLogger.warn("RAO computation has been interrupted");
        final RaoSuccessResponse raoResponse = new RaoSuccessResponse.Builder()
                .withId(raoRequest.getId())
                .withInterrupted(true)
                .build();
        sendRaoResponse(raoResponse, replyTo, brokerCorrelationId);
    }

    private void sendRaoFailedResponse(final Exception exception, final String replyTo, final String correlationId) {
        LOGGER.error("Exception occurred while running RAO", exception);
        final RaoRunnerException raoRunnerException;
        if (exception instanceof RaoRunnerException rre) {
            raoRunnerException = rre;
        } else {
            raoRunnerException = new RaoRunnerException("Unhandled exception: " + exception.getMessage(), exception);
        }
        final Message errorMessage = createFailedResponse(raoRunnerException, correlationId);
        sendMessage(replyTo, errorMessage);
    }

    private void sendRaoResponse(final AbstractRaoResponse raoResponse, final String replyTo, final String correlationId) {
        final Message responseMessage = createMessageFromRaoResponse(raoResponse, correlationId);
        sendMessage(replyTo, responseMessage);
    }

    private Message createFailedResponse(final RaoRunnerException exception, final String correlationId) {
        final RaoFailureResponse response = new RaoFailureResponse.Builder()
                .withId("defaultId")
                .withErrorMessage(exception.getMessage())
                .build();
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(response))
            .andProperties(buildMessageResponseProperties(correlationId, response.isRaoFailed()))
            .build();
    }

    private Message createMessageFromRaoResponse(final AbstractRaoResponse raoResponse, final String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(raoResponse))
                .andProperties(buildMessageResponseProperties(correlationId, raoResponse.isRaoFailed()))
                .build();
    }

    private void sendMessage(final String replyTo, final Message responseMessage) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, responseMessage);
        } else {
            amqpTemplate.send(amqpConfiguration.raoResponseExchange().getName(), "", responseMessage);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Response message: {}", new String(responseMessage.getBody()));
        }
    }

    private MessageProperties buildMessageResponseProperties(final String correlationId, final boolean failed) {
        return MessagePropertiesBuilder.newInstance()
            .setAppId(APPLICATION_ID)
            .setContentEncoding(CONTENT_ENCODING)
            .setContentType(CONTENT_TYPE)
            .setCorrelationId(correlationId)
            .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
            .setExpiration(amqpConfiguration.raoResponseExpiration())
            .setPriority(PRIORITY)
            .setHeaderIfAbsent("rao-failure", failed)
            .build();
    }
}
