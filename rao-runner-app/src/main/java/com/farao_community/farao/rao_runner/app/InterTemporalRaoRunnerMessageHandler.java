/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.InterTemporalRaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.AmqpConfiguration;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class InterTemporalRaoRunnerMessageHandler extends AbstractRaoRunnerMessageHandler<InterTemporalRaoRunnerService> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterTemporalRaoRunnerMessageHandler.class);

    public InterTemporalRaoRunnerMessageHandler(InterTemporalRaoRunnerService raoRunnerService, AmqpTemplate amqpTemplate, AmqpConfiguration amqpConfiguration, FanoutExchange raoResponseExchange, Logger businessLogger, RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        super(raoRunnerService, amqpTemplate, amqpConfiguration, raoResponseExchange, businessLogger, restTemplateBuilder, urlConfiguration);
    }

    @Override
    public void handleMessage(Message message) {
        final String replyTo = message.getMessageProperties().getReplyTo();
        final String brokerCorrelationId = message.getMessageProperties().getCorrelationId();

        try {
            final InterTemporalRaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), InterTemporalRaoRequest.class);
            LOGGER.info("InterTemporal RAO request received: {}", raoRequest);
            if (interruptionServerIsActivated && checkIsInterrupted(raoRequest)) {
                sendRaoInterruptedResponse(raoRequest, replyTo, brokerCorrelationId);
                return;
            }
            addMetaDataToLogsModelContext(raoRequest.getId(), brokerCorrelationId, message.getMessageProperties().getAppId(), raoRequest.getEventPrefix());
            final GenericThreadLauncher<InterTemporalRaoRunnerService, AbstractRaoResponse> launcher = new GenericThreadLauncher<>(
                raoRunnerService,
                raoRequest.getRunId(),
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
            System.gc(); // NOSONAR because memory management is crucial for rao-runner, therefore suggesting to the JVM to collect garbage here should not be considered as a problem by Sonar
        } catch (RaoRunnerException e) {
            sendRaoFailedResponse(e, replyTo, brokerCorrelationId);
        } catch (Exception e) {
            final RaoRunnerException wrappingException = new RaoRunnerException("Unhandled exception: " + e.getMessage(), e);
            sendRaoFailedResponse(wrappingException, replyTo, brokerCorrelationId);
        }
    }
}
