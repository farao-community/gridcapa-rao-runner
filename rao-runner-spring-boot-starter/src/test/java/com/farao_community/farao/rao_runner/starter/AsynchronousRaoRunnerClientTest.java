/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import org.junit.jupiter.api.DisplayName;
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
 * @author Daniel THIRION {@literal <daniel.thirion at rte-france.com>}
 */
class AsynchronousRaoRunnerClientTest {

    @Test
    void testRunRaoAsynchronouslySuccess() throws IOException {
        // Given
        final AsyncAmqpTemplate amqpTemplate = mock(AsyncAmqpTemplate.class);
        final RaoRunnerClientProperties properties = mock(RaoRunnerClientProperties.class, RETURNS_DEEP_STUBS);
        final RaoRequest raoRequest = mock(RaoRequest.class);
        final Message message = mock(Message.class);

        when(properties.getAmqp().getQueueName()).thenReturn("queue-test");
        when(amqpTemplate.sendAndReceive(eq("queue-test"), any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(message));
        Mockito.when(message.getBody()).thenReturn(getClass().getResourceAsStream("/raoResponseMessage.json").readAllBytes());

        final AsynchronousRaoRunnerClient client = new AsynchronousRaoRunnerClient(amqpTemplate, properties);

        //When
        final CompletableFuture<AbstractRaoResponse> resultFuture = client.runRaoAsynchronously(raoRequest);

        //Then
        assertNotNull(resultFuture, "La future réponse ne doit pas être nulle");
        assertDoesNotThrow(resultFuture::join);

        verify(amqpTemplate).sendAndReceive(eq("queue-test"), any(Message.class));
        verify(properties.getAmqp()).getQueueName();
    }

    @Test
    void testRunRaoAsynchronouslyConversionFailure() {
        //Given
        final AsyncAmqpTemplate amqpTemplate = mock(AsyncAmqpTemplate.class);
        final RaoRunnerClientProperties properties = mock(RaoRunnerClientProperties.class, RETURNS_DEEP_STUBS);
        final RaoRequest raoRequest = mock(RaoRequest.class);
        final Message message = mock(Message.class);

        when(properties.getAmqp().getQueueName()).thenReturn("queue-test");
        when(amqpTemplate.sendAndReceive(eq("queue-test"), any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(message));

        final AsynchronousRaoRunnerClient client = new AsynchronousRaoRunnerClient(amqpTemplate, properties);

        //When & Then
        assertThrows(RuntimeException.class, () -> client.runRaoAsynchronously(raoRequest).join());

        verify(amqpTemplate).sendAndReceive(eq("queue-test"), any(Message.class));
        verify(properties.getAmqp()).getQueueName();
    }
}
