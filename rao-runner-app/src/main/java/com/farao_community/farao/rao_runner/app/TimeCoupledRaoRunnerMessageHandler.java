/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
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

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class TimeCoupledRaoRunnerMessageHandler extends AbstractRaoRunnerMessageHandler<TimeCoupledRaoRunnerService> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeCoupledRaoRunnerMessageHandler.class);

    public TimeCoupledRaoRunnerMessageHandler(TimeCoupledRaoRunnerService raoRunnerService, AmqpTemplate amqpTemplate, AmqpConfiguration amqpConfiguration, FanoutExchange raoResponseExchange, Logger businessLogger, RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        super(raoRunnerService, amqpTemplate, amqpConfiguration, raoResponseExchange, businessLogger, restTemplateBuilder, urlConfiguration);
    }

    @Override
    public void handleMessage(Message message) {
        final String replyTo = message.getMessageProperties().getReplyTo();
        final String brokerCorrelationId = message.getMessageProperties().getCorrelationId();

        try {
            final TimeCoupledRaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), TimeCoupledRaoRequest.class);
            LOGGER.info("Time-coupled RAO request received: {}", raoRequest);
            if (interruptionServerIsActivated && checkIsInterrupted(raoRequest)) {
                sendRaoInterruptedResponse(raoRequest, replyTo, brokerCorrelationId);
                return;
            }
            addMetaDataToLogsModelContext(raoRequest.getId(), brokerCorrelationId, message.getMessageProperties().getAppId(), raoRequest.getEventPrefix());
            final GenericThreadLauncher<TimeCoupledRaoRunnerService, AbstractRaoResponse> launcher = new GenericThreadLauncher<>(
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
}
