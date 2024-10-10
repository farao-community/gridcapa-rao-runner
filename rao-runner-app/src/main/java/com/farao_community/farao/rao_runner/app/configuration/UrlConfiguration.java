/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@ConfigurationProperties(prefix = "rao-runner.url")
public class UrlConfiguration {
    private final List<String> whitelist = new ArrayList<>();
    private String interruptServerUrl;

    public List<String> getWhitelist() {
        return whitelist;
    }

    public String getInterruptServerUrl() {
        return interruptServerUrl;
    }

    public void setInterruptServerUrl(String interruptServerUrl) {
        this.interruptServerUrl = interruptServerUrl;
    }
}

