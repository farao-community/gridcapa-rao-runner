/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoSuccessResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class TimeCoupledRaoRunnerClientTest {

    private final JsonApiConverter jsonConverter = new JsonApiConverter();

    @Test
    void raoRunnerClientSuccessTest() throws IOException {
        final AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        final TimeCoupledRaoRunnerClient client = new TimeCoupledRaoRunnerClient(amqpTemplate, buildProperties());
        final TimeCoupledRaoRequest raoRequest = jsonConverter.fromJsonMessage(getClass().getResourceAsStream("/timeCoupledRaoRequestMessage.json").readAllBytes(), TimeCoupledRaoRequest.class);

        final Message responseMessage = Mockito.mock(Message.class);
        Mockito.when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/timeCoupledRaoResponseMessage.json").readAllBytes());
        final MessageProperties messageProperties = new MessageProperties();
        Mockito.when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-queue"), messageArgumentCaptor.capture())).thenReturn(responseMessage);

        final AbstractRaoResponse raoResponse = client.runRao(raoRequest);

        Assertions.assertThat(raoResponse).isInstanceOf(TimeCoupledRaoSuccessResponse.class);
        final TimeCoupledRaoSuccessResponse raoSuccessResponse = (TimeCoupledRaoSuccessResponse) raoResponse;
        Assertions.assertThat(raoSuccessResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoSuccessResponse.getInstant()).contains("instant");
        Assertions.assertThat(raoSuccessResponse.getNetworksWithPraFileUrl()).isEqualTo("networksWithPraFileUrl");
        Assertions.assertThat(raoSuccessResponse.getRaoResultsFileUrl()).isEqualTo("raoResultsFileUrl");
        Assertions.assertThat(messageArgumentCaptor.getValue().getMessageProperties().getReceivedRoutingKey()).isEqualTo("TIME-COUPLED");
    }

    @Test
    void raoRunnerClientFailTest() throws IOException {
        final AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        final TimeCoupledRaoRunnerClient client = new TimeCoupledRaoRunnerClient(amqpTemplate, buildProperties());
        final TimeCoupledRaoRequest raoRequest = jsonConverter.fromJsonMessage(getClass().getResourceAsStream("/timeCoupledRaoRequestMessage.json").readAllBytes(), TimeCoupledRaoRequest.class);

        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-queue"), Mockito.any())).thenReturn(null);

        final AbstractRaoResponse raoResponse = client.runRao(raoRequest);

        Assertions.assertThat(raoResponse).isInstanceOf(RaoFailureResponse.class);
        final RaoFailureResponse raoFailureResponse = (RaoFailureResponse) raoResponse;
        Assertions.assertThat(raoFailureResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoFailureResponse.getErrorMessage()).isEqualTo("Rao Runner server did not respond");
    }

    private RaoRunnerClientProperties buildProperties() {
        final RaoRunnerClientProperties properties = new RaoRunnerClientProperties();
        final RaoRunnerClientProperties.AmqpConfiguration amqpConfiguration = new RaoRunnerClientProperties.AmqpConfiguration();
        amqpConfiguration.setQueueName("my-queue");
        amqpConfiguration.setExpiration("10000");
        properties.setAmqp(amqpConfiguration);
        return properties;
    }
}
