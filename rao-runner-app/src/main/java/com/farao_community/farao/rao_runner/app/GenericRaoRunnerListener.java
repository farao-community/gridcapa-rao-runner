/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class GenericRaoRunnerListener implements MessageListener {

    private final TimeCoupledRaoRunnerMessageHandler timeCoupledRaoRunnerMessageHandler;
    private final RaoRunnerMessageHandler raoRunnerMessageHandler;

    public GenericRaoRunnerListener(final TimeCoupledRaoRunnerMessageHandler timeCoupledRaoRunnerMessageHandler, final RaoRunnerMessageHandler raoRunnerMessageHandler) {
        this.timeCoupledRaoRunnerMessageHandler = timeCoupledRaoRunnerMessageHandler;
        this.raoRunnerMessageHandler = raoRunnerMessageHandler;
    }

    @Override
    public void onMessage(Message message) {
        if (message.getMessageProperties().getReceivedRoutingKey().equals("TIME-COUPLED")) {
            timeCoupledRaoRunnerMessageHandler.handleMessage(message);
        } else {
            raoRunnerMessageHandler.handleMessage(message);
        }
    }
}
