/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@Configuration
public class AmqpConfiguration {

    @Value("${rao-runner.messages.rao-response.exchange}")
    private String raoResponseExchange;

    @Value("${rao-runner.messages.rao-response.expiration}")
    private String raoResponseExpiration;

    @Value("${rao-runner.messages.rao-request.queue-name}")
    private String raoRequestQueueName;

    @Value("${rao-runner.messages.rao-request.delivery-limit}")
    private int deliveryLimit;

    public String raoResponseExpiration() {
        return raoResponseExpiration;
    }

    public String getRaoResponseExchange() {
        return raoResponseExchange;
    }

    public String getRaoRequestQueueName() {
        return raoRequestQueueName;
    }

    public int getDeliveryLimit() {
        return deliveryLimit;
    }
}
