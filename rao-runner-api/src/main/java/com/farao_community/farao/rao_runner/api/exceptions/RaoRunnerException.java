/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.exceptions;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@JsonTypeName("rao-runner-exception")
public class RaoRunnerException extends AbstractRaoRunnerException {

    private static final int STATUS = 400;
    private static final String CODE = "400-Rao-Runner-Exception";

    public RaoRunnerException(String message) {
        super(message);
    }

    public RaoRunnerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    @Override
    public int getStatus() {
        return STATUS;
    }

    @Override
    public String getCode() {
        return CODE;
    }
}
