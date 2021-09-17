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
import org.springframework.amqp.core.Message;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class RaoResponseConversionHelper {

    private RaoResponseConversionHelper() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static RaoResponse convertRaoResponse(Message message, JsonApiConverter jsonConverter) {
        try {
            return jsonConverter.fromJsonMessage(message.getBody(), RaoResponse.class);
        } catch (ResourceParseException resourceParseException) {
            // exception details from rao-runner app is wrapped into a ResourceParseException on json Api Error format.
            String originCause = resourceParseException.getErrors().getErrors().get(0).getDetail();
            throw new RaoRunnerException(originCause);
        } catch (Exception unknownException) {
            throw new RaoRunnerException("Unsupported exception thrown by rao-runner app", unknownException);
        }
    }
}
