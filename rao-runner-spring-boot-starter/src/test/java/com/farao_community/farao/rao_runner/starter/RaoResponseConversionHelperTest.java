/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class RaoResponseConversionHelperTest {

    @Test
    void convertRaoFailureResponse() {
        final JsonApiConverter jsonApiConverter = new JsonApiConverter();
        final RaoFailureResponse failureResponse = new RaoFailureResponse.Builder().withId("testId").build();
        final byte[] bytes = jsonApiConverter.toJsonMessage(failureResponse);
        final MessageProperties properties = new MessageProperties();
        properties.setHeader("rao-failure", true);
        final Message message = MessageBuilder.withBody(bytes).andProperties(properties).build();

        final AbstractRaoResponse abstractRaoResponse = RaoResponseConversionHelper.convertRaoResponse(message, jsonApiConverter);

        assertNotNull(abstractRaoResponse);
        assertInstanceOf(RaoFailureResponse.class, abstractRaoResponse);
        final RaoFailureResponse castRaoResponse = (RaoFailureResponse) abstractRaoResponse;
        assertEquals("testId", castRaoResponse.getId());
        assertTrue(castRaoResponse.isRaoFailed());
    }

    @Test
    void convertRaoSuccessResponse() {
        final JsonApiConverter jsonApiConverter = new JsonApiConverter();
        final RaoSuccessResponse successResponse = new RaoSuccessResponse.Builder().withId("testId").build();
        final byte[] bytes = jsonApiConverter.toJsonMessage(successResponse);
        final Message message = MessageBuilder.withBody(bytes).build();

        final AbstractRaoResponse abstractRaoResponse = RaoResponseConversionHelper.convertRaoResponse(message, jsonApiConverter);

        assertNotNull(abstractRaoResponse);
        assertInstanceOf(RaoSuccessResponse.class, abstractRaoResponse);
        final RaoSuccessResponse castRaoResponse = (RaoSuccessResponse) abstractRaoResponse;
        assertEquals("testId", castRaoResponse.getId());
        assertFalse(castRaoResponse.isRaoFailed());
    }

    @Test
    void convertInvalidResponse() {
        final JsonApiConverter jsonApiConverter = new JsonApiConverter();
        final Message message = MessageBuilder.withBody(new byte[]{}).build();

        assertThrows(RaoRunnerException.class, () -> RaoResponseConversionHelper.convertRaoResponse(message, jsonApiConverter));
    }
}
