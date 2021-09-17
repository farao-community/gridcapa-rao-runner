/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.exceptions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoRunnerExceptionSerializer extends JsonSerializer<RaoRunnerException> {
    @Override
    public void serialize(RaoRunnerException e, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("status", e.getStatus());
        jsonGenerator.writeStringField("code", e.getCode());
        jsonGenerator.writeStringField("title", e.getTitle());
        jsonGenerator.writeStringField("details", e.getDetails());
        jsonGenerator.writeEndObject();
    }
}
