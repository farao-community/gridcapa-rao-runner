/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record ThreadLauncherResult<U>(U result, Exception exception, boolean isInterrupted) {

    public boolean hasError() {
        return exception != null;
    }

    public static <U> ThreadLauncherResult<U> success(U result) {
        return new ThreadLauncherResult<>(result, null, false);
    }

    public static <U> ThreadLauncherResult<U> interrupt() {
        return new ThreadLauncherResult<>(null, null, true);
    }

    public static <U> ThreadLauncherResult<U> error(Exception e) {
        return new ThreadLauncherResult<>(null, e, false);
    }
}
