/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResponseConversionHelperTest {

    @Test
    void checkThatExceptionIsConvertedCorrectly() throws IOException {
        Message responseMessage = mock(Message.class);
        when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/raoResponseMessage.json").readAllBytes());
        JsonApiConverter jsonApiConverter = mock(JsonApiConverter.class);
        Errors errors = new Errors();
        Error error = new Error();
        error.setDetail("exception conversion test");
        errors.setErrors(Collections.singletonList(error));
        when(jsonApiConverter.fromJsonMessage(responseMessage.getBody(), RaoResponse.class)).thenThrow(new ResourceParseException(errors));
        Exception exception = assertThrows(RaoRunnerException.class, () -> RaoResponseConversionHelper.convertRaoResponse(responseMessage, jsonApiConverter));
        String expectedMessage = "exception conversion test";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
}
