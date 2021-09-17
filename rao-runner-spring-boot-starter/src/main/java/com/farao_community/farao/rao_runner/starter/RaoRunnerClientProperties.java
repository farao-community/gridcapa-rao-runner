/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@ConfigurationProperties("rao-runner-server")
public class RaoRunnerClientProperties {

    private AmqpConfiguration amqp;

    public AmqpConfiguration getAmqp() {
        return amqp;
    }

    public void setAmqp(AmqpConfiguration amqp) {
        this.amqp = amqp;
    }

    public static class AmqpConfiguration {
        private String queueName;
        private String clientAppId;
        private String expiration;

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getClientAppId() {
            return clientAppId;
        }

        public void setClientAppId(String clientAppId) {
            this.clientAppId = clientAppId;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

    }
}
