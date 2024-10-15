/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
class GenericThreadLauncherTest {

    private static class LaunchWithoutThreadableAnnotation {
        public Integer run(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result *= i * i;
            }
            return result;
        }
    }

    private static class LaunchWithMultipleThreadableAnnotation {
        @Threadable
        public Integer run(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result *= i * i;
            }
            return result;
        }

        @Threadable
        public Integer run2(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result += i * i;
            }
            return result;
        }
    }

    private static class LaunchWithThreadableAnnotation {
        @Threadable
        public Integer run(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result *= i;
            }
            return result;
        }
    }

    @Test
    void launchGenericThread() {
        GenericThreadLauncher<LaunchWithThreadableAnnotation, Integer> gtl = new GenericThreadLauncher<>(
                new LaunchWithThreadableAnnotation(),
                "withThreadable",
                Collections.emptyMap(),
                10);

        gtl.start();

        Optional<Thread> th = Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals("withThreadable"))
                .findFirst();
        Assertions.assertThat(th).isPresent();

        ThreadLauncherResult<Integer> result = gtl.getResult();
        Assertions.assertThat(result.result()).isEqualTo(362880);
    }

    @Test
    void testNotAnnotatedClass() {
        final Map<String, String> contextMap = Collections.emptyMap();
        final LaunchWithoutThreadableAnnotation annotation = new LaunchWithoutThreadableAnnotation();
        Assertions.assertThatExceptionOfType(RaoRunnerException.class)
                .isThrownBy(() -> new GenericThreadLauncher<>(annotation, "withThreadable", contextMap, 10))
                .withMessage("The class com.farao_community.farao.rao_runner.app.GenericThreadLauncherTest.LaunchWithoutThreadableAnnotation has no method annotated with @Threadable");
    }

    @Test
    void testMultipleAnnotatedClass() {
        final LaunchWithMultipleThreadableAnnotation annotation = new LaunchWithMultipleThreadableAnnotation();
        final Map<String, String> contextMap = Collections.emptyMap();
        Assertions.assertThatExceptionOfType(RaoRunnerException.class)
                .isThrownBy(() -> new GenericThreadLauncher<>(annotation, "withThreadable", contextMap, 10))
                .withMessage("The class com.farao_community.farao.rao_runner.app.GenericThreadLauncherTest.LaunchWithMultipleThreadableAnnotation must have only one method annotated with @Threadable");
    }
}

