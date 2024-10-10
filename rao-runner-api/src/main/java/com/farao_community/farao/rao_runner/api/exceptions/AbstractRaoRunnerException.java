/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.exceptions;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public abstract class AbstractRaoRunnerException extends RuntimeException {

    protected AbstractRaoRunnerException(String message) {
        super(message);
    }

    protected AbstractRaoRunnerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public abstract int getStatus();

    public abstract String getCode();

    public final String getTitle() {
        return getMessage();
    }

    public final String getDetails() {
        String message = getMessage();
        Throwable cause = getCause();
        if (cause == null) {
            return message;
        }
        StringBuilder sb = new StringBuilder(64);
        if (message != null) {
            sb.append(message).append("; ");
        }
        sb.append("nested exception is ").append(cause);
        return sb.toString();
    }
}


