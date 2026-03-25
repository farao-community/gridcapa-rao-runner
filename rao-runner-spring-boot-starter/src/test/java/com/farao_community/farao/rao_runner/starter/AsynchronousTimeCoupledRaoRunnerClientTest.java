/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class AsynchronousTimeCoupledRaoRunnerClientTest {

    @Test
    void testRunRaoAsynchronouslySuccess() throws IOException {
        // Given
        final AsyncAmqpTemplate amqpTemplate = mock(AsyncAmqpTemplate.class);
        final RaoRunnerClientProperties properties = mock(RaoRunnerClientProperties.class, RETURNS_DEEP_STUBS);
        final TimeCoupledRaoRequest raoRequest = mock(TimeCoupledRaoRequest.class);
        final Message message = mock(Message.class);

        when(properties.getAmqp().getQueueName()).thenReturn("queue-test");
        when(amqpTemplate.sendAndReceive(eq("queue-test"), any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(message));
        Mockito.when(message.getBody()).thenReturn(getClass().getResourceAsStream("/timeCoupledRaoResponseMessage.json").readAllBytes());

        final AsynchronousTimeCoupledRaoRunnerClient client = new AsynchronousTimeCoupledRaoRunnerClient(amqpTemplate, properties);

        //When
        final CompletableFuture<AbstractRaoResponse> resultFuture = client.runRaoAsynchronously(raoRequest);

        //Then
        assertNotNull(resultFuture);
        assertDoesNotThrow(resultFuture::join);

        verify(amqpTemplate).sendAndReceive(eq("queue-test"), any(Message.class));
        verify(properties.getAmqp()).getQueueName();
    }

    @Test
    void testRunRaoAsynchronouslyConversionFailure() {
        //Given
        final AsyncAmqpTemplate amqpTemplate = mock(AsyncAmqpTemplate.class);
        final RaoRunnerClientProperties properties = mock(RaoRunnerClientProperties.class, RETURNS_DEEP_STUBS);
        final TimeCoupledRaoRequest raoRequest = mock(TimeCoupledRaoRequest.class);
        final Message message = mock(Message.class);

        when(properties.getAmqp().getQueueName()).thenReturn("queue-test");
        when(amqpTemplate.sendAndReceive(eq("queue-test"), any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(message));

        final AsynchronousTimeCoupledRaoRunnerClient client = new AsynchronousTimeCoupledRaoRunnerClient(amqpTemplate, properties);

        //When & Then
        final CompletableFuture<AbstractRaoResponse> resultFuture = client.runRaoAsynchronously(raoRequest);
        assertThrows(RuntimeException.class, resultFuture::join);

        verify(amqpTemplate).sendAndReceive(eq("queue-test"), any(Message.class));
        verify(properties.getAmqp()).getQueueName();
    }
}
