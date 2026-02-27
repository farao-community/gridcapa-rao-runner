/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class TestFileImporter extends FileImporter {
    public TestFileImporter(UrlConfiguration urlConfiguration) {
        super(urlConfiguration);
    }

    @Override
    RaoParameters importRaoParameters(String raoParametersFileUrl) throws FileImporterException {
        final String url = raoParametersFileUrl.startsWith("file:") ? raoParametersFileUrl : "file:" + raoParametersFileUrl;
        return super.importRaoParameters(url);
    }

    @Override
    TimeCoupledConstraints importIcsFile(String icsFileUrl) throws FileImporterException {
        final String url = icsFileUrl.startsWith("file:") ? icsFileUrl : "file:" + icsFileUrl;
        return super.importIcsFile(url);
    }

    @Override
    public Network importNetwork(final String networkFileUrl) throws FileImporterException {
        final String url = networkFileUrl.startsWith("file:") ? networkFileUrl : "file:" + networkFileUrl;
        return super.importNetwork(url);
    }

    @Override
    public Crac importCracWithContext(final String cracFileUrl, final Network network) throws FileImporterException {
        final String url = cracFileUrl.startsWith("file:") ? cracFileUrl : "file:" + cracFileUrl;
        return super.importCracWithContext(url, network);
    }
}
