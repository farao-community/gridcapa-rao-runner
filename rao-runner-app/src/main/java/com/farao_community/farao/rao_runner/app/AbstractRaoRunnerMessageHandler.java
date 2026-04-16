/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.RaoRunnerConstants;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.app.configuration.AmqpConfiguration;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public abstract class AbstractRaoRunnerMessageHandler<RAO_RUNNER_SERVICE_TYPE extends AbstractRaoRunnerService> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractRaoRunnerMessageHandler.class);
    protected static final String APPLICATION_ID = "rao-runner-server";

    protected final JsonApiConverter jsonApiConverter;
    protected final RAO_RUNNER_SERVICE_TYPE raoRunnerService;
    protected final AmqpTemplate amqpTemplate;
    protected final AmqpConfiguration amqpConfiguration;
    protected final FanoutExchange raoResponseExchange;
    protected final Logger businessLogger;
    protected final RestTemplateBuilder restTemplateBuilder;
    protected final UrlConfiguration urlConfiguration;

    @Value("${rao-runner.with-interruption-server}")
    protected boolean interruptionServerIsActivated;

    protected AbstractRaoRunnerMessageHandler(RAO_RUNNER_SERVICE_TYPE raoRunnerService, AmqpTemplate amqpTemplate, AmqpConfiguration amqpConfiguration, FanoutExchange raoResponseExchange, Logger businessLogger, RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        this.raoResponseExchange = raoResponseExchange;
        this.businessLogger = businessLogger;
        this.jsonApiConverter = new JsonApiConverter();
        this.raoRunnerService = raoRunnerService;
        this.amqpTemplate = amqpTemplate;
        this.amqpConfiguration = amqpConfiguration;
        this.restTemplateBuilder = restTemplateBuilder;
        this.urlConfiguration = urlConfiguration;
    }

    protected abstract void handleMessage(Message message);

    protected void handleMessage(Message message, Class<? extends AbstractRaoRequest> raoRequestClass) {
        final String replyTo = message.getMessageProperties().getReplyTo();
        final String brokerCorrelationId = message.getMessageProperties().getCorrelationId();

        try {
            final AbstractRaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), raoRequestClass);
            LOGGER.info("RAO request received: {}", raoRequest);
            if (interruptionServerIsActivated && checkIsInterrupted(raoRequest)) {
                sendRaoInterruptedResponse(raoRequest, replyTo, brokerCorrelationId);
                return;
            }
            addMetaDataToLogsModelContext(raoRequest.getId(), brokerCorrelationId, message.getMessageProperties().getAppId(), raoRequest.getEventPrefix());
            final GenericThreadLauncher<RAO_RUNNER_SERVICE_TYPE, AbstractRaoResponse> launcher = new GenericThreadLauncher<>(
                raoRunnerService,
                raoRequest.getRunId(),
                MDC.getCopyOfContextMap(),
                raoRequest
            );

            startRaoComputation(launcher, raoRequest, replyTo, brokerCorrelationId);
        } catch (RaoRunnerException e) {
            sendRaoFailedResponse(e, replyTo, brokerCorrelationId);
        } catch (Exception e) {
            final RaoRunnerException wrappingException = new RaoRunnerException("Unhandled exception: " + e.getMessage(), e);
            sendRaoFailedResponse(wrappingException, replyTo, brokerCorrelationId);
        }
    }

    protected boolean checkIsInterrupted(final AbstractRaoRequest raoRequest) {
        final String requestUrl = urlConfiguration.getInterruptServerUrl() + raoRequest.getRunId();
        final ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, Boolean.class);
        return responseEntity.getBody() != null
            && responseEntity.getStatusCode() == HttpStatus.OK
            && responseEntity.getBody();
    }

    protected void addMetaDataToLogsModelContext(final String gridcapaTaskId, final String computationId, final String clientAppId, final Optional<String> optPrefix) {
        MDC.put("gridcapaTaskId", gridcapaTaskId);
        MDC.put("computationId", computationId);
        MDC.put("clientAppId", clientAppId);
        if (optPrefix.isPresent()) {
            MDC.put("eventPrefix", optPrefix.get());
        } else {
            MDC.remove("eventPrefix");
        }
    }

    protected void sendRaoInterruptedResponse(final AbstractRaoRequest raoRequest, final String replyTo, final String brokerCorrelationId) {
        businessLogger.warn("RAO computation has been interrupted");
        final RaoSuccessResponse raoResponse = new RaoSuccessResponse.Builder()
                .withId(raoRequest.getId())
                .withInterrupted(true)
                .build();
        sendRaoResponse(raoResponse, replyTo, brokerCorrelationId);
    }

    protected void sendRaoFailedResponse(final Exception exception, final String replyTo, final String correlationId) {
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

    protected void sendRaoResponse(final AbstractRaoResponse raoResponse, final String replyTo, final String correlationId) {
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
            amqpTemplate.send(raoResponseExchange.getName(), "", responseMessage);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Response message: {}", new String(responseMessage.getBody()));
        }
    }

    private MessageProperties buildMessageResponseProperties(final String correlationId, final boolean failed) {
        return MessagePropertiesBuilder.newInstance()
            .setAppId(APPLICATION_ID)
            .setContentEncoding(RaoRunnerConstants.CONTENT_ENCODING)
            .setContentType(RaoRunnerConstants.CONTENT_TYPE)
            .setCorrelationId(correlationId)
            .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
            .setExpiration(amqpConfiguration.raoResponseExpiration())
            .setPriority(RaoRunnerConstants.DEFAULT_PRIORITY)
            .setHeaderIfAbsent("rao-failure", failed)
            .build();
    }

    protected void startRaoComputation(final GenericThreadLauncher<RAO_RUNNER_SERVICE_TYPE, ? extends AbstractRaoResponse> launcher,
                                       final AbstractRaoRequest raoRequest,
                                       final String replyTo,
                                       final String brokerCorrelationId) throws Exception {
        businessLogger.info("Starting the RAO computation");
        launcher.start();

        final ThreadLauncherResult<? extends AbstractRaoResponse> raoThreadResult = launcher.getResult();
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
        System.gc(); // NOSONAR because memory management is crucial for rao-runner, therefore suggesting to the JVM to collect garbage here should not be considered as a problem by Sonar
    }
}
