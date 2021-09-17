/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerExceptionSerializer;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * JSON API conversion component
 * Allows automatic conversion from resources or exceptions towards JSON API formatted bytes.
 *
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class JsonApiConverter {

    private final ObjectMapper objectMapper;

    public JsonApiConverter() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(RaoRunnerException.class, new RaoRunnerExceptionSerializer());
        objectMapper.registerModule(module);
    }

    public <T> T fromJsonMessage(byte[] jsonMessage, Class<T> clazz) throws IOException {
        return objectMapper.readValue(jsonMessage, clazz);
    }

    public <T> byte[] toJsonMessage(T jsonApiObject) {
        try {
            return objectMapper.writeValueAsBytes(jsonApiObject);
        } catch (JsonProcessingException e) {
            throw new RaoRunnerException("Exception occurred during message conversion", e);
        }
    }
}
