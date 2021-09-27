/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.springframework.amqp.core.*;

import java.util.concurrent.CompletableFuture;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class AsynchronousRaoRunnerClient {

    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";

    private final RaoRunnerClientProperties raoRunnerClientProperties;
    private final JsonApiConverter jsonConverter;
    private final AsyncAmqpTemplate asyncAmqpTemplate;

    public AsynchronousRaoRunnerClient(AsyncAmqpTemplate asyncAmqpTemplate, RaoRunnerClientProperties raoRunnerClientProperties) {
        this.raoRunnerClientProperties = raoRunnerClientProperties;
        this.asyncAmqpTemplate = asyncAmqpTemplate;
        this.jsonConverter = new JsonApiConverter();
    }

    public CompletableFuture<RaoResponse> runRaoAsynchronously(RaoRequest raoRequest) {
        return asyncAmqpTemplate.sendAndReceive(raoRunnerClientProperties.getAmqp().getQueueName(), buildMessage(raoRequest))
                .completable().thenApply(message -> RaoResponseConversionHelper.convertRaoResponse(message, jsonConverter));
    }

    private Message buildMessage(RaoRequest raoRequest) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(raoRequest))
                .andProperties(buildMessageProperties())
                .build();
    }

    private MessageProperties buildMessageProperties() {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(raoRunnerClientProperties.getAmqp().getClientAppId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(raoRunnerClientProperties.getAmqp().getExpiration())
                .build();
    }
}
