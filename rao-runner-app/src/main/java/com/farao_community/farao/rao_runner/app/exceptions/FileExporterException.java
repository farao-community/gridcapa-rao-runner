/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.exceptions;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class FileExporterException extends Exception {

    public FileExporterException(String message) {
        super(message);
    }

    public FileExporterException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
