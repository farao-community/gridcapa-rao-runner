/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerListenerTest {

    @Autowired
    private RaoRunnerListener raoRunnerListener;

    @Test
    void checkThatMdcMetadataIsPropagatedCorrectly() {
        Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        raoRunnerListener.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.of("prefix"));
        logger.info("message");
        assertEquals(4, listAppender.list.get(0).getMDCPropertyMap().size());
        assertEquals("process-id", listAppender.list.get(0).getMDCPropertyMap().get("gridcapaTaskId"));
        assertEquals("request-id", listAppender.list.get(0).getMDCPropertyMap().get("computationId"));
        assertEquals("client-id", listAppender.list.get(0).getMDCPropertyMap().get("clientAppId"));
        assertEquals("prefix", listAppender.list.get(0).getMDCPropertyMap().get("eventPrefix"));
    }

    @Test
    void checkThatMdcMetadataIsPropagatedCorrectlyWithoutPrefix() {
        Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        raoRunnerListener.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.empty());
        logger.info("message");
        assertEquals(3, listAppender.list.get(0).getMDCPropertyMap().size());
        assertEquals("process-id", listAppender.list.get(0).getMDCPropertyMap().get("gridcapaTaskId"));
        assertEquals("request-id", listAppender.list.get(0).getMDCPropertyMap().get("computationId"));
        assertEquals("client-id", listAppender.list.get(0).getMDCPropertyMap().get("clientAppId"));
        assertNull(listAppender.list.get(0).getMDCPropertyMap().get("eventPrefix"));
    }
}
