/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import com.farao_community.farao.rao_runner.app.RaoRunnerListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@Configuration
public class AmqpBeans {
    private final AmqpConfiguration amqpConfiguration;

    public AmqpBeans(final AmqpConfiguration amqpConfiguration) {
        this.amqpConfiguration = amqpConfiguration;
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange("rao-request.DLX");
    }

    @Bean
    Queue dlq() {
        return QueueBuilder.durable(amqpConfiguration.getRaoRequestQueueName() + ".dlq").build();
    }

    @Bean
    Binding dLQbinding() {
        return BindingBuilder.bind(dlq()).to(deadLetterExchange()).with("rao-request.DLK");
    }

    @Bean
    public Queue raoRequestQueue() {
        QueueBuilder queueBuilder = QueueBuilder.durable(amqpConfiguration.getRaoRequestQueueName());
        queueBuilder.deadLetterExchange("rao-request.DLX").deadLetterRoutingKey("rao-request.DLK");
        queueBuilder.deliveryLimit(amqpConfiguration.getDeliveryLimit());
        return queueBuilder.quorum().build();
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, Queue raoRequestQueue, RaoRunnerListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(raoRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        simpleMessageListenerContainer.setPrefetchCount(1);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange raoResponseExchange() {
        return new FanoutExchange(amqpConfiguration.getRaoResponseExchange());
    }
}
