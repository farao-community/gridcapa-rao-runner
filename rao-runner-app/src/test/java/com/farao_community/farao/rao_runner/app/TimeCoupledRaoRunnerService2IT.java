/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import com.farao_community.farao.rao_runner.app.exceptions.FileExporterException;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TimeCoupledRaoRunnerService2IT {

    @Autowired
    private TimeCoupledRaoRunnerService raoRunnerService;
    @TestBean
    private FileImporter fileImporter;
    @MockitoBean
    private FileExporter fileExporter;

    private static FileImporterForTesting fileImporter() {
        final UrlConfiguration urlConfiguration = new UrlConfiguration();
        urlConfiguration.getWhitelist().add("file:/");
        return new FileImporterForTesting(urlConfiguration);
    }

    @Test
    void getRaoInputTest() throws FileExporterException {
        final TimeCoupledRaoRequest simpleRaoRequest = TimeCoupledTestHelper.getValidTimeCoupledRaoRequest("");

        Mockito.when(fileExporter.saveTimeCoupledRaoResult(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("raoResultUrl");
        Mockito.when(fileExporter.saveNetworks(Mockito.any(), Mockito.any())).thenReturn("networksUrl");

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runTimeCoupledRao(simpleRaoRequest);

        final ArgumentCaptor<TimeCoupledRaoResult> raoResultArgumentCaptor = ArgumentCaptor.forClass(TimeCoupledRaoResult.class);
        Mockito.verify(fileExporter).saveTimeCoupledRaoResult(raoResultArgumentCaptor.capture(), Mockito.any(), Mockito.any());

        Assertions.assertThat(abstractRaoResponse)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", "raoRequestId")
            .hasFieldOrPropertyWithValue("networksWithPraFileUrl", "networksUrl")
            .hasFieldOrPropertyWithValue("raoResultsFileUrl", "raoResultUrl");

        Assertions.assertThat(raoResultArgumentCaptor.getValue().getTimestamps()).hasSize(4);
    }
}
