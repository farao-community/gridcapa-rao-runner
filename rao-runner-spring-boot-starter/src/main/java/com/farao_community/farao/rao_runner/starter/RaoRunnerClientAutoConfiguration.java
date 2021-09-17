/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Configuration
@EnableConfigurationProperties(RaoRunnerClientProperties.class)
public class RaoRunnerClientAutoConfiguration {

    private final RaoRunnerClientProperties clientProperties;

    public RaoRunnerClientAutoConfiguration(RaoRunnerClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public RaoRunnerClient raoRunnerClient(AmqpTemplate amqpTemplate) {
        return new RaoRunnerClient(amqpTemplate, clientProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AsyncAmqpTemplate.class)
    public AsynchronousRaoRunnerClient asynchronousRaoRunnerClient(AsyncAmqpTemplate asyncAmqpTemplate) {
        return new AsynchronousRaoRunnerClient(asyncAmqpTemplate, clientProperties);
    }

}
