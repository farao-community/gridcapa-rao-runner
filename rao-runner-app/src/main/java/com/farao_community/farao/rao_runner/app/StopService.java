/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StopService.class);

    public void stop(final String runId) {
        LOGGER.info("Received stop request for run id {}", runId);
        Optional<Thread> thread = isRunning(runId);
        if (thread.isPresent()) {
            LOGGER.info("Run has been found, stopping it");
            while (thread.isPresent()) {
                thread.get().interrupt();
                thread = isRunning(runId);
            }
            LOGGER.info("Computation for run id {} has been successfully stopped", runId);
        } else {
            LOGGER.info("Computation for run id {} has not been found", runId);
        }
    }

    Optional<Thread> isRunning(final String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .findFirst();
    }

}
