/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.springframework.amqp.core.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoRunnerClient {

    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int DEFAULT_PRIORITY = 1;

    private final AmqpTemplate amqpTemplate;
    private final RaoRunnerClientProperties raoRunnerClientProperties;
    private final JsonApiConverter jsonConverter;

    public RaoRunnerClient(AmqpTemplate amqpTemplate, RaoRunnerClientProperties raoRunnerClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.raoRunnerClientProperties = raoRunnerClientProperties;
        this.jsonConverter = new JsonApiConverter();
    }

    public RaoResponse runRao(RaoRequest raoRequest, int priority) {
        Message responseMessage = amqpTemplate.sendAndReceive(raoRunnerClientProperties.getAmqp().getQueueName(), buildMessage(raoRequest, priority));
        if (responseMessage != null) {
            return RaoResponseConversionHelper.convertRaoResponse(responseMessage, jsonConverter);
        } else {
            throw new RaoRunnerException("Rao Runner server did not respond");
        }
    }

    public RaoResponse runRao(RaoRequest raoRequest) {
        return runRao(raoRequest, DEFAULT_PRIORITY);
    }

    private Message buildMessage(RaoRequest raoRequest, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(raoRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(raoRunnerClientProperties.getAmqp().getClientAppId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(raoRunnerClientProperties.getAmqp().getExpiration())
                .setPriority(priority)
                .build();
    }
}
