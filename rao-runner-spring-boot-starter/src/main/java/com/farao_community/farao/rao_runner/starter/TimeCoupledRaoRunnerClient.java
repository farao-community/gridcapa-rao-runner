/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.RaoRunnerConstants;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class TimeCoupledRaoRunnerClient {
    private final RaoRunnerClientProperties.AmqpConfiguration amqpConfiguration;
    private final AmqpTemplate amqpTemplate;
    private final JsonApiConverter jsonConverter;

    public TimeCoupledRaoRunnerClient(AmqpTemplate amqpTemplate, RaoRunnerClientProperties raoRunnerClientProperties) {
        this.amqpConfiguration = raoRunnerClientProperties.getAmqp();
        this.amqpTemplate = amqpTemplate;
        this.jsonConverter = new JsonApiConverter();
    }

    public AbstractRaoResponse runRao(final TimeCoupledRaoRequest raoRequest, final int priority) {
        final Message responseMessage = amqpTemplate.sendAndReceive(amqpConfiguration.getQueueName(), buildMessage(raoRequest, priority));
        if (responseMessage != null) {
            return RaoResponseConversionHelper.convertTimeCoupledRaoResponse(responseMessage, jsonConverter);
        } else {
            return new RaoFailureResponse.Builder()
                    .withId(raoRequest.getId())
                    .withErrorMessage("Rao Runner server did not respond")
                    .build();
        }
    }

    public AbstractRaoResponse runRao(final TimeCoupledRaoRequest raoRequest) {
        return runRao(raoRequest, RaoRunnerConstants.DEFAULT_PRIORITY);
    }

    private Message buildMessage(final TimeCoupledRaoRequest raoRequest, final int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(raoRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(final int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(amqpConfiguration.getClientAppId())
                .setContentEncoding(RaoRunnerConstants.CONTENT_ENCODING)
                .setContentType(RaoRunnerConstants.CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpConfiguration.getExpiration())
                .setPriority(priority)
                .setReceivedRoutingKey(RaoRunnerConstants.TIME_COUPLED_ROUTING_KEY)
                .build();
    }
}
