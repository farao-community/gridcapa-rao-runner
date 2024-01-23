/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;
import com.powsybl.openrao.raoapi.Rao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Component
public class GeneralBeans {

    @Bean
    public Rao.Runner raoRunnerProvider() {
        return Rao.find();
    }

    @Bean
    public Logger getLogger() {
        return  LoggerFactory.getLogger("RAO_RUNNER_BUSINESS_LOGGER");
    }

}
