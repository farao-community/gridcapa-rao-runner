/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Configuration
public class RaoRunnerEventsLogging {

    @Value("logback.metadata.gridcapa-task-id")
    private String gridcapaTaskIdLogsMetaData;
    @Value("logback.metadata.rao-request-id")
    private  String raoRequestIdLogsMetaData;
    @Value("logback.metadata.client-app-id")
    private  String clientAppLogsMetaData;

    @Bean
    public Logger getLogger() {
        return  LoggerFactory.getLogger("RAO_RUNNER_BUSINESS_LOGGER");
    }

    public void addMetaDataToLogsModelContext(String gridcapaTaskId, String raoRequestId, String clientAppId) {
        MDC.put(gridcapaTaskIdLogsMetaData, gridcapaTaskId);
        MDC.put(raoRequestIdLogsMetaData, raoRequestId);
        MDC.put(clientAppLogsMetaData, clientAppId);
    }
}
