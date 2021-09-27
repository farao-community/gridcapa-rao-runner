/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.springframework.stereotype.Service;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class RaoLauncherService {

    public RaoResult run(RaoInput raoInput, RaoParameters raoParameters) {
        return Rao.run(raoInput, raoParameters);
    }
}
