package com.farao_community.farao.rao_runner.app.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;


/**
 * Component that logs Spring Boot env information upon application startup.
 */
@Component
public class EnvPropertiesLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvPropertiesLogger.class);

    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent event) {
        ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext()
            .getEnvironment();

        // Properties by source
        LOGGER.info("Properties by Source");
        for (PropertySource<?> propertySource : env.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> eps) {
                for (String propertyName : eps.getPropertyNames()) {
                    Object value = eps.getProperty(propertyName);
                    LOGGER.info("{}: {}={}", propertySource.getName(), propertyName, value);
                }
            } else {
                LOGGER.info("{} - cant print", propertySource.getName());
            }
        }

        // Final merged properties
        LOGGER.info("Merged properties");
        Map<String, String> mergedProperties = new TreeMap<>();
        Map<String, String> propertyOrigins = new HashMap<>();

        // Iterate property sources in reverse to respect Spring's override rules
        List<PropertySource<?>> sources = new ArrayList<>();
        env.getPropertySources().forEach(sources::add);
        Collections.reverse(sources);

        for (PropertySource<?> propertySource : sources) {
            if (propertySource instanceof EnumerablePropertySource<?> eps) {
                for (String name : eps.getPropertyNames()) {
                    Object value = eps.getProperty(name);
                    if (value != null) {
                        mergedProperties.put(name, value.toString());
                        propertyOrigins.put(name, propertySource.getName());
                    }
                }
            }
        }

        for (Map.Entry<String, String> entry : mergedProperties.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            String source = propertyOrigins.get(name);
            LOGGER.info("{}: {}={}", source, name, value);
        }

    }

}
