/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StopService {

    public void stop(String taskId) {
        Optional<Thread> thread = isRunning(taskId);
        while (thread.isPresent()) {
            thread.get().interrupt();
            thread = isRunning(taskId);
        }
    }

    Optional<Thread> isRunning(String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .findFirst();
    }

}
