/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Daniel THIRION {@literal <daniel.thirion at rte-france.com>}
 */
class MDCAwareForkJoinPoolTest {

    @Test
    @DisplayName("submit(Callable<T>) should wrap task with MDC context and execute it")
    void submitCallableShouldWrapAndExecuteWithMdc() throws Exception {
        // Given
        final Callable<String> callableTask;
        final Map<String, String> mdcContext;
        final ForkJoinTask<String> resultTask;
        try (final MDCAwareForkJoinPool pool = new MDCAwareForkJoinPool()) {
            callableTask = mock(Callable.class);
            when(callableTask.call()).thenReturn("testResult");

            mdcContext = new HashMap<>();
            mdcContext.put("key", "value");
            MDC.setContextMap(mdcContext);

            // When
            resultTask = pool.submit(callableTask);
        }
        final String result = resultTask.get();

        // Then
        assertEquals("testResult", result);
        verify(callableTask, times(1)).call();
        assertEquals(mdcContext, MDC.getCopyOfContextMap());
    }

    @Test
    @DisplayName("submit(Runnable, T) should wrap task with MDC context and execute it")
    void submitRunnableWithResultShouldWrapAndExecuteWithMdc() throws Exception {
        // Given
        final Runnable runnableTask;
        final String expectedResult;
        final Map<String, String> mdcContext;
        final ForkJoinTask<String> resultTask;
        try (final MDCAwareForkJoinPool pool = new MDCAwareForkJoinPool()) {
            runnableTask = mock(Runnable.class);
            expectedResult = "testResult";

            mdcContext = new HashMap<>();
            mdcContext.put("key", "value");
            MDC.setContextMap(mdcContext);

            // When
            resultTask = pool.submit(runnableTask, expectedResult);
        }
        final String result = resultTask.get();

        // Then
        assertEquals(expectedResult, result);
        verify(runnableTask, times(1)).run();
        assertEquals(mdcContext, MDC.getCopyOfContextMap());
    }

    @Test
    @DisplayName("submit(Runnable) should wrap task with MDC context and execute it")
    void submitRunnableShouldWrapAndExecuteWithMdc() throws Exception {
        // Given
        final Runnable runnableTask;
        final Map<String, String> mdcContext;
        final ForkJoinTask<?> resultTask;
        try (final MDCAwareForkJoinPool pool = new MDCAwareForkJoinPool()) {
            runnableTask = mock(Runnable.class);

            mdcContext = new HashMap<>();
            mdcContext.put("key", "value");
            MDC.setContextMap(mdcContext);

            // When
            resultTask = pool.submit(runnableTask);
        }
        resultTask.get();

        // Then
        verify(runnableTask, times(1)).run();
        assertEquals(mdcContext, MDC.getCopyOfContextMap());
    }

}
