/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public abstract class AbstractRaoResponse {
    @JsonIgnore
    protected boolean raoFailed;

    public boolean isRaoFailed() {
        return raoFailed;
    }
}
