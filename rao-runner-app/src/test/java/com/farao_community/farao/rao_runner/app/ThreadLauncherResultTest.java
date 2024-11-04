/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class ThreadLauncherResultTest {
    @Test
    void buildSuccessTest() {
        final Object result = new Object();

        final ThreadLauncherResult<Object> successResult = ThreadLauncherResult.success(result);

        Assertions.assertThat(successResult).isNotNull();
        Assertions.assertThat(successResult.result()).isEqualTo(result);
        Assertions.assertThat(successResult.isInterrupted()).isFalse();
        Assertions.assertThat(successResult.hasError()).isFalse();
        Assertions.assertThat(successResult.exception()).isNull();
    }

    @Test
    void buildInterruptTest() {
        final ThreadLauncherResult<Object> interruptResult = ThreadLauncherResult.interrupt();

        Assertions.assertThat(interruptResult).isNotNull();
        Assertions.assertThat(interruptResult.result()).isNull();
        Assertions.assertThat(interruptResult.isInterrupted()).isTrue();
        Assertions.assertThat(interruptResult.hasError()).isFalse();
        Assertions.assertThat(interruptResult.exception()).isNull();
    }

    @Test
    void buildErrorTest() {
        final Exception exception = new Exception();

        final ThreadLauncherResult<Object> errorResult = ThreadLauncherResult.error(exception);

        Assertions.assertThat(errorResult).isNotNull();
        Assertions.assertThat(errorResult.result()).isNull();
        Assertions.assertThat(errorResult.isInterrupted()).isFalse();
        Assertions.assertThat(errorResult.hasError()).isTrue();
        Assertions.assertThat(errorResult.exception()).isEqualTo(exception);
    }
}
