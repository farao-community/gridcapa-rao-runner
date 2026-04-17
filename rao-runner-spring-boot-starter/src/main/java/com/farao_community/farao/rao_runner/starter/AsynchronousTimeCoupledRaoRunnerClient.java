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
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class AsynchronousTimeCoupledRaoRunnerClient {
    private final RaoRunnerClientProperties.AmqpConfiguration amqpConfiguration;
    private final AsyncAmqpTemplate asyncAmqpTemplate;
    private final JsonApiConverter jsonConverter;

    public AsynchronousTimeCoupledRaoRunnerClient(AsyncAmqpTemplate asyncAmqpTemplate, RaoRunnerClientProperties raoRunnerClientProperties) {
        this.amqpConfiguration = raoRunnerClientProperties.getAmqp();
        this.asyncAmqpTemplate = asyncAmqpTemplate;
        this.jsonConverter = new JsonApiConverter();
    }

    public CompletableFuture<AbstractRaoResponse> runRaoAsynchronously(final TimeCoupledRaoRequest raoRequest) {
        return asyncAmqpTemplate.sendAndReceive(amqpConfiguration.getQueueName(), buildMessage(raoRequest))
            .thenApplyAsync(
                message -> RaoResponseConversionHelper.convertTimeCoupledRaoResponse(message, jsonConverter),
                new MDCAwareForkJoinPool()
            );
    }

    private Message buildMessage(final TimeCoupledRaoRequest raoRequest) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(raoRequest))
            .andProperties(buildMessageProperties())
            .build();
    }

    private MessageProperties buildMessageProperties() {
        return MessagePropertiesBuilder.newInstance()
            .setAppId(amqpConfiguration.getClientAppId())
            .setContentEncoding(RaoRunnerConstants.CONTENT_ENCODING)
            .setContentType(RaoRunnerConstants.CONTENT_TYPE)
            .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
            .setExpiration(amqpConfiguration.getExpiration())
            .setReceivedRoutingKey(RaoRunnerConstants.TIME_COUPLED_ROUTING_KEY)
            .build();
    }
}
