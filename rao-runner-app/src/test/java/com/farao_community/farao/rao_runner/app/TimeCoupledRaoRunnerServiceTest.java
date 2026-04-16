/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoSuccessResponse;
import com.farao_community.farao.rao_runner.api.resource.TimedInput;
import com.farao_community.farao.rao_runner.app.exceptions.FileExporterException;
import com.farao_community.farao.rao_runner.app.exceptions.FileImporterException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.TimeCoupledRao;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TimeCoupledRaoRunnerServiceTest {

    @Autowired
    private TimeCoupledRaoRunnerService raoRunnerService;
    @MockitoBean
    private FileExporter fileExporter;
    @MockitoBean
    private FileImporter fileImporter;
    @MockitoBean
    private TimeCoupledRao.Runner raoRunnerProvider;

    @Test
    void raoThrowFIETest() throws FileImporterException {
        final TimeCoupledRaoRequest simpleRaoRequest = TimeCoupledTestHelper.getValidTimeCoupledRaoRequest("file:");
        Mockito.when(fileImporter.importRaoParameters(Mockito.any())).thenThrow(new FileImporterException("It's a test", null));

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runTimeCoupledRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
            .isNotNull()
            .hasFieldOrPropertyWithValue("raoFailed", true);

        final RaoFailureResponse raoFailureResponse = (RaoFailureResponse) abstractRaoResponse;
        Assertions.assertThat(raoFailureResponse)
            .hasFieldOrPropertyWithValue("id", "raoRequestId")
            .hasFieldOrPropertyWithValue("errorMessage", "Exception occurred in rao-runner: It's a test");
    }

    @Test
    void raoThrowORETest() throws FileImporterException {
        final TimeCoupledRaoRequest simpleRaoRequest = TimeCoupledTestHelper.getValidTimeCoupledRaoRequest("file:");
        final Crac crac = Mockito.mock(Crac.class);
        Mockito.when(fileImporter.importRaoParameters(Mockito.any())).thenReturn(new RaoParameters());
        Mockito.when(fileImporter.importIcsFile(Mockito.any())).thenReturn(new TimeCoupledConstraints());
        final Network network = Mockito.mock(Network.class);
        Mockito.when(network.getVariantManager()).thenReturn(Mockito.mock(VariantManager.class));
        Mockito.when(fileImporter.importNetwork(Mockito.any())).thenReturn(network);
        Mockito.when(fileImporter.importCracWithContext(Mockito.any(), Mockito.any())).thenReturn(crac);
        Mockito.when(crac.getTimestamp()).thenReturn(Optional.of(OffsetDateTime.now()));
        Mockito.when(raoRunnerProvider.run(Mockito.any(), Mockito.any())).thenThrow(new OpenRaoException("It's a test"));

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runTimeCoupledRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
            .isNotNull()
            .hasFieldOrPropertyWithValue("raoFailed", true);

        final RaoFailureResponse raoFailureResponse = (RaoFailureResponse) abstractRaoResponse;
        Assertions.assertThat(raoFailureResponse)
            .hasFieldOrPropertyWithValue("id", "raoRequestId")
            .hasFieldOrPropertyWithValue("errorMessage", "RAO exception occurred: It's a test");
    }

    @Test
    void raoFailureTest() throws FileImporterException {
        final TimeCoupledRaoRequest simpleRaoRequest = TimeCoupledTestHelper.getValidTimeCoupledRaoRequest("file:");
        final Crac crac = Mockito.mock(Crac.class);
        Mockito.when(fileImporter.importRaoParameters(Mockito.any())).thenReturn(new RaoParameters());
        Mockito.when(fileImporter.importIcsFile(Mockito.any())).thenReturn(new TimeCoupledConstraints());
        final Network network = Mockito.mock(Network.class);
        Mockito.when(network.getVariantManager()).thenReturn(Mockito.mock(VariantManager.class));
        Mockito.when(fileImporter.importNetwork(Mockito.any())).thenReturn(network);
        Mockito.when(fileImporter.importCracWithContext(Mockito.any(), Mockito.any())).thenReturn(crac);
        Mockito.when(crac.getTimestamp()).thenReturn(Optional.of(OffsetDateTime.now()));

        final TimeCoupledRaoResult timeCoupledRaoResult = Mockito.mock(TimeCoupledRaoResult.class);
        Mockito.when(raoRunnerProvider.run(Mockito.any(), Mockito.any())).thenReturn(timeCoupledRaoResult);
        Mockito.when(timeCoupledRaoResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runTimeCoupledRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
            .isNotNull()
            .hasFieldOrPropertyWithValue("raoFailed", true);

        final RaoFailureResponse raoFailureResponse = (RaoFailureResponse) abstractRaoResponse;
        Assertions.assertThat(raoFailureResponse)
            .hasFieldOrPropertyWithValue("id", "raoRequestId")
            .hasFieldOrPropertyWithValue("errorMessage", "RAO computation failed");
    }

    @Test
    void checkSuccessfulSimpleRaoRun() throws FileImporterException, FileExporterException {
        final TimeCoupledRaoRequest simpleRaoRequest = TimeCoupledTestHelper.getValidTimeCoupledRaoRequest("file:");
        final RaoParameters raoParameters = new RaoParameters();
        final TimeCoupledConstraints timeCoupledConstraints = new TimeCoupledConstraints();

        Mockito.when(fileImporter.importRaoParameters(simpleRaoRequest.getRaoParametersFileUrl())).thenReturn(raoParameters);
        Mockito.when(fileImporter.importIcsFile(simpleRaoRequest.getIcsFileUrl())).thenReturn(timeCoupledConstraints);
        for (TimedInput timedInput : simpleRaoRequest.getTimedInputs()) {
            final Network network = Mockito.mock(Network.class);
            Mockito.when(network.getVariantManager()).thenReturn(Mockito.mock(VariantManager.class));
            Mockito.when(fileImporter.importNetwork(timedInput.networkFileUrl())).thenReturn(network);
            final Crac crac = Mockito.mock(Crac.class);
            Mockito.when(crac.getTimestamp()).thenReturn(Optional.of(timedInput.timestamp()));
            Mockito.when(fileImporter.importCracWithContext(timedInput.cracFileUrl(), network)).thenReturn(crac);
            fileImporter.importCracWithContext(timedInput.cracFileUrl(), network);
        }

        final TimeCoupledRaoResult timeCoupledRaoResult = Mockito.mock(TimeCoupledRaoResult.class);
        Mockito.when(raoRunnerProvider.run(Mockito.any(), Mockito.any())).thenReturn(timeCoupledRaoResult);
        Mockito.when(timeCoupledRaoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);

        Mockito.when(fileExporter.saveTimeCoupledRaoResult(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("raoResultsUrl");
        Mockito.when(fileExporter.saveNetworks(Mockito.any(), Mockito.any())).thenReturn("networksUrl");

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runTimeCoupledRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
            .isNotNull()
            .hasFieldOrPropertyWithValue("raoFailed", false);
        final TimeCoupledRaoSuccessResponse raoResponse = (TimeCoupledRaoSuccessResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
            .hasFieldOrPropertyWithValue("id", "raoRequestId")
            .hasFieldOrPropertyWithValue("networksWithPraFileUrl", "networksUrl")
            .hasFieldOrPropertyWithValue("raoResultsFileUrl", "raoResultsUrl")
            .hasFieldOrPropertyWithValue("interrupted", false);
        checkComputationStartAndEndInstants(raoResponse);
    }

    private static void checkComputationStartAndEndInstants(final TimeCoupledRaoSuccessResponse raoResponse) {
        final Instant now = Instant.now();
        Assertions.assertThat(raoResponse.getComputationStartInstant())
            .isBetween(now.minus(1, ChronoUnit.MINUTES), now);
        Assertions.assertThat(raoResponse.getComputationEndInstant())
            .isBetween(now.minus(1, ChronoUnit.MINUTES), now);
        Assertions.assertThat(raoResponse.getComputationStartInstant())
            .isBeforeOrEqualTo(raoResponse.getComputationEndInstant());
    }
}
