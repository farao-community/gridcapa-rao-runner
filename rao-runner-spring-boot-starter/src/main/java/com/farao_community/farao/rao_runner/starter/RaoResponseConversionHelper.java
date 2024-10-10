/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
import org.springframework.amqp.core.Message;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class RaoResponseConversionHelper {

    private RaoResponseConversionHelper() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static AbstractRaoResponse convertRaoResponse(final Message message, final JsonApiConverter jsonConverter) {
        try {
            if (isFailureMessage(message)) {
                return jsonConverter.fromJsonMessage(message.getBody(), RaoFailureResponse.class);
            } else {
                return jsonConverter.fromJsonMessage(message.getBody(), RaoSuccessResponse.class);
            }
        } catch (Exception unknownException) {
            throw new RaoRunnerException("Unsupported exception thrown by rao-runner app", unknownException);
        }
    }

    private static boolean isFailureMessage(final Message message) {
        return message.getMessageProperties() != null
                && message.getMessageProperties().getHeaders().containsKey("rao-failure")
                && (boolean) message.getMessageProperties().getHeader("rao-failure");
    }
}
