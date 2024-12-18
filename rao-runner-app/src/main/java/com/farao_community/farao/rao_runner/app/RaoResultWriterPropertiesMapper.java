/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.commons.Unit;

import java.util.Properties;

/**
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
public final class RaoResultWriterPropertiesMapper {

    private RaoResultWriterPropertiesMapper() {
        //static class
    }

    /**
     *
     * @param unit
     * @return
     */
    static Properties generateJsonProperties(final Unit unit) {
        final Properties properties = new Properties();
        final String propertiesPrefix = "rao-result.export.json.flows-in-";
        if (Unit.AMPERE == unit) {
            properties.setProperty(propertiesPrefix + "amperes", "true");
        } else {
            properties.setProperty(propertiesPrefix + "megawatts", "true");
        }
        return properties;
    }
}
