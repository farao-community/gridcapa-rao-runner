/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerServiceTest {

    @Autowired
    RaoRunnerService raoRunnerService;
    @MockBean
    Rao.Runner raoRunnerProvider;
    @MockBean
    FileImporter fileImporter;
    @MockBean
    FileExporter fileExporter;

    private RaoParameters raoParameters;
    private Network network;
    private Crac crac;

    @BeforeEach
    public void setUp() throws IOException, FileImporterException {
        raoParameters = new RaoParameters();
        network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        crac = Crac.read("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")), network);

        when(fileImporter.importRaoParameters(any())).thenReturn(raoParameters);
        when(fileImporter.importNetwork(any())).thenReturn(network);
        when(fileImporter.importCrac(any(), any())).thenReturn(crac);
    }

    @Test
    void checkFailedSimpleRaoRun() {
        final RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withNetworkFileUrl("http://host:9000/network.xiidm")
                .withCracFileUrl("http://host:9000/crac.json")
                .withRaoParametersFileUrl("http://host:9000/raoParameters.json")
                .build();
        final RaoResult raoResult = mock(RaoResult.class);

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
        when(raoRunnerProvider.run(any(), any())).thenReturn(raoResult);

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("raoFailed", true);
        final RaoFailureResponse raoResponse = (RaoFailureResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
                .hasFieldOrPropertyWithValue("id", "id")
                .hasFieldOrPropertyWithValue("errorMessage", "RAO computation failed");
    }

    @Test
    void checkSuccessfulSimpleRaoRun() {
        final RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withNetworkFileUrl("http://host:9000/network.xiidm")
                .withCracFileUrl("http://host:9000/crac.json")
                .withRaoParametersFileUrl("http://host:9000/raoParameters.json")
                .build();
        final RaoResult raoResult = mock(RaoResult.class);

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(fileExporter.saveNetwork(network, simpleRaoRequest)).thenReturn("simple-networkWithPRA-url");
        when(fileExporter.saveRaoResult(eq(raoResult), eq(crac), eq(simpleRaoRequest), any())).thenReturn("simple-RaoResultJson-url");

        final ArgumentCaptor<RaoInput> raoInputCaptor = ArgumentCaptor.forClass(RaoInput.class);
        when(raoRunnerProvider.run(raoInputCaptor.capture(), eq(raoParameters))).thenReturn(raoResult);

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runRao(simpleRaoRequest);

        Assertions.assertThat(raoInputCaptor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("crac", crac)
                .hasFieldOrPropertyWithValue("network", network)
                .hasFieldOrPropertyWithValue("glsk", null)
                .hasFieldOrPropertyWithValue("referenceProgram", null);
        Assertions.assertThat(abstractRaoResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("raoFailed", false);
        final RaoSuccessResponse raoResponse = (RaoSuccessResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
                .hasFieldOrPropertyWithValue("id", "id")
                .hasFieldOrPropertyWithValue("instant", Optional.empty())
                .hasFieldOrPropertyWithValue("networkWithPraFileUrl", "simple-networkWithPRA-url")
                .hasFieldOrPropertyWithValue("cracFileUrl", "http://host:9000/crac.json")
                .hasFieldOrPropertyWithValue("raoResultFileUrl", "simple-RaoResultJson-url")
                .hasFieldOrPropertyWithValue("interrupted", false);
        checkComputationStartAndEndInstants(raoResponse);
    }

    @Test
    void checkSuccessfulCoreRaoRun() throws FileImporterException {
        final RaoRequest coreRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withInstant("2019-01-08T12:30:00Z")
                .withNetworkFileUrl("http://host:9000/network.xiidm")
                .withCracFileUrl("http://host:9000/crac.json")
                .withRefprogFileUrl("http://host:9000/refProg.xml")
                .withRealGlskFileUrl("http://host:9000/glsk.xml")
                .withVirtualhubsFileUrl("http://host:9000/virtualhubs.xml")
                .withRaoParametersFileUrl("raoParameters.json")
                .withResultsDestination("destination-key")
                .withTargetEndInstant(Instant.MAX)
                .build();
        final RaoResult raoResult = mock(RaoResult.class);

        final InputStream refProgFileInputStream = getClass().getResourceAsStream("/rao_inputs/refprog.xml");
        final InputStream glskFileInputStream = getClass().getResourceAsStream("/rao_inputs/glsk.xml");
        final GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(Objects.requireNonNull(glskFileInputStream));
        final InputStream virtualhubsFileInputStream = getClass().getResourceAsStream("/rao_inputs/virtualHubsConfigurationFile.xml");

        final ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgFileInputStream, OffsetDateTime.parse("2019-01-08T12:30:00Z"));
        final ZonalData<SensitivityVariableSet> glsks = ucteGlskProvider.getZonalGlsks(network, OffsetDateTime.parse("2019-01-08T12:30:00Z").toInstant());
        final VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(virtualhubsFileInputStream);

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(fileImporter.importRefProg(coreRaoRequest.getInstant().get(), coreRaoRequest.getRefprogFileUrl().get()))
                .thenReturn(referenceProgram);
        when(fileImporter.importGlsk(coreRaoRequest.getInstant().get(), coreRaoRequest.getRealGlskFileUrl().get(), network))
                .thenReturn(glsks);
        when(fileImporter.importVirtualHubs(coreRaoRequest.getVirtualhubsFileUrl().get())).thenReturn(virtualHubsConfiguration);

        when(fileExporter.saveNetwork(network, coreRaoRequest)).thenReturn("simple-networkWithPRA-url");
        when(fileExporter.saveRaoResult(raoResult, crac, coreRaoRequest, RaoParameters.load().getObjectiveFunctionParameters().getUnit())).thenReturn("simple-RaoResultJson-url");

        final ArgumentCaptor<RaoInput> raoInputCaptor = ArgumentCaptor.forClass(RaoInput.class);
        when(raoRunnerProvider.run(raoInputCaptor.capture(), eq(raoParameters))).thenReturn(raoResult);

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runRao(coreRaoRequest);

        Assertions.assertThat(raoInputCaptor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("crac", crac)
                .hasFieldOrPropertyWithValue("network", network)
                .hasFieldOrPropertyWithValue("glsk", glsks)
                .hasFieldOrPropertyWithValue("referenceProgram", referenceProgram);
        Assertions.assertThat(abstractRaoResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("raoFailed", false);
        final RaoSuccessResponse raoResponse = (RaoSuccessResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
                .hasFieldOrPropertyWithValue("id", "id")
                .hasFieldOrPropertyWithValue("instant", Optional.of("2019-01-08T12:30:00Z"))
                .hasFieldOrPropertyWithValue("networkWithPraFileUrl", "simple-networkWithPRA-url")
                .hasFieldOrPropertyWithValue("cracFileUrl", "http://host:9000/crac.json")
                .hasFieldOrPropertyWithValue("raoResultFileUrl", "simple-RaoResultJson-url")
                .hasFieldOrPropertyWithValue("interrupted", false);
        checkComputationStartAndEndInstants(raoResponse);
    }

    private static void checkComputationStartAndEndInstants(final RaoSuccessResponse raoResponse) {
        final Instant now = Instant.now();
        Assertions.assertThat(raoResponse.getComputationStartInstant())
                .isBetween(now.minus(1, ChronoUnit.MINUTES), now);
        Assertions.assertThat(raoResponse.getComputationEndInstant())
                .isBetween(now.minus(1, ChronoUnit.MINUTES), now);
        Assertions.assertThat(raoResponse.getComputationStartInstant())
                .isBeforeOrEqualTo(raoResponse.getComputationEndInstant());
    }

    @Test
    void runRaoThrowsOpenRaoException() {
        final RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withNetworkFileUrl("http://host:9000/network.xiidm")
                .withCracFileUrl("http://host:9000/crac.json")
                .withRaoParametersFileUrl("http://host:9000/raoParameters.json")
                .build();

        final OpenRaoException testException = new OpenRaoException("This is a test");
        when(raoRunnerProvider.run(any(), any())).thenThrow(testException);

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("raoFailed", true);
        final RaoFailureResponse raoResponse = (RaoFailureResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
                .hasFieldOrPropertyWithValue("id", "id")
                .hasFieldOrPropertyWithValue("errorMessage", "FARAO exception occurred when running rao: This is a test");
    }

    @Test
    void runRaoThrowsFileImporterException() throws FileImporterException {
        final RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .build();

        final FileImporterException exception = new FileImporterException("Testing...", null);
        when(fileImporter.importRaoParameters(any())).thenThrow(exception);

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("raoFailed", true);
        final RaoFailureResponse raoResponse = (RaoFailureResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
                .hasFieldOrPropertyWithValue("id", "id")
                .hasFieldOrPropertyWithValue("errorMessage", "Exception occurred in rao-runner: Testing...");
    }
}
