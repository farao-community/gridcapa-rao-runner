/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api;

import com.farao_community.farao.rao_runner.api.exceptions.AbstractRaoRunnerException;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.SerializationFeature;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.models.errors.Error;

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
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    public <T> T fromJsonMessage(byte[] jsonMessage, Class<T> clazz) {
        ResourceConverter converter = createConverter();
        return converter.readDocument(jsonMessage, clazz).get();
    }

    public <T> byte[] toJsonMessage(T jsonApiObject) {
        ResourceConverter converter = createConverter();
        JSONAPIDocument<?> jsonApiDocument = jsonApiObject instanceof Error jsonApiObjectError
            ? new JSONAPIDocument<>(jsonApiObjectError)
            : new JSONAPIDocument<>(jsonApiObject);

        try {
            return converter.writeDocument(jsonApiDocument);
        } catch (DocumentSerializationException e) {
            throw new RaoRunnerException("Exception occurred during message conversion", e);
        }
    }

    public byte[] toJsonMessage(AbstractRaoRunnerException exception) {
        return toJsonMessage(convertExceptionToJsonError(exception));
    }

    private ResourceConverter createConverter() {
        ResourceConverter converter = new ResourceConverter(objectMapper, RaoRequest.class, RaoResponse.class);
        converter.disableSerializationOption(SerializationFeature.INCLUDE_META);
        return converter;
    }

    private Error convertExceptionToJsonError(AbstractRaoRunnerException exception) {
        Error error = new Error();
        error.setStatus(Integer.toString(exception.getStatus()));
        error.setCode(exception.getCode());
        error.setTitle(exception.getTitle());
        error.setDetail(exception.getDetails());
        return error;
    }
}
