/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultImporter;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerServiceTest {

    @Autowired
    RaoRunnerService raoRunnerService;
    @MockBean
    Rao.Runner raoRunnerProv;
    @MockBean
    FileImporter fileImporter;
    @MockBean
    FileExporter fileExporter;

    Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
    Crac crac;
    RaoResult raoResult;
    ZonalData<SensitivityVariableSet> glsks;
    ReferenceProgram referenceProgram;
    VirtualHubsConfiguration virtualHubsConfiguration;

    @BeforeEach
    public void setUp() {
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao_inputs/raoResult.json");
        crac = CracImporters.importCrac("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")), network);
        raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        Mockito.when(raoRunnerProv.run(Mockito.any(), Mockito.any())).thenReturn(raoResult);

        InputStream glskFileInputStream = getClass().getResourceAsStream("/rao_inputs/glsk.xml");
        GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(Objects.requireNonNull(glskFileInputStream));
        InputStream refProgFileInputStream = getClass().getResourceAsStream("/rao_inputs/refprog.xml");
        InputStream virtualhubsFileInputStream = getClass().getResourceAsStream("/rao_inputs/virtualHubsConfigurationFile.xml");

        glsks = ucteGlskProvider.getZonalGlsks(network, OffsetDateTime.parse("2019-01-08T12:30:00Z").toInstant());
        referenceProgram = RefProgImporter.importRefProg(refProgFileInputStream, OffsetDateTime.parse("2019-01-08T12:30:00Z"));
        virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(virtualhubsFileInputStream);

        Mockito.when(fileImporter.importNetwork(Mockito.any())).thenReturn(network);
        Mockito.when(fileImporter.importCrac(Mockito.any(), Mockito.any())).thenReturn(crac);
    }

    @Test
    void checkSimpleRaoRun() {
        RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withNetworkFileUrl("http://host:9000/network.xiidm")
                .withCracFileUrl("http://host:9000/crac.json")
                .withRaoParametersFileUrl("http://host:9000/raoParameters.json")
                .build();

        Mockito.when(fileExporter.saveNetwork(network, simpleRaoRequest)).thenReturn("simple-networkWithPRA-url");
        Mockito.when(fileExporter.saveRaoResult(raoResult, crac, simpleRaoRequest, RaoParameters.load().getObjectiveFunctionParameters().getType().getUnit())).thenReturn("simple-RaoResultJson-url");
        Mockito.when(fileImporter.importRaoParameters(simpleRaoRequest.getRaoParametersFileUrl())).thenReturn(new RaoParameters());

        RaoResponse raoResponse = raoRunnerService.runRao(simpleRaoRequest);
        assertEquals("id", raoResponse.getId());
        assertEquals("http://host:9000/crac.json", raoResponse.getCracFileUrl());
        assertEquals("simple-networkWithPRA-url", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("simple-RaoResultJson-url", raoResponse.getRaoResultFileUrl());
        assertEquals(Optional.empty(), raoResponse.getInstant());
    }

    @Test
    void checkCoreRaoRun() {
        RaoRequest coreRaoRequest = new RaoRequest.RaoRequestBuilder()
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

        Mockito.when(fileImporter.importRaoParameters(coreRaoRequest.getRaoParametersFileUrl())).thenReturn(new RaoParameters());
        Mockito.when(fileImporter.importRefProg(coreRaoRequest.getInstant().get(), coreRaoRequest.getRefprogFileUrl().get()))
                .thenReturn(referenceProgram);
        Mockito.when(fileImporter.importGlsk(coreRaoRequest.getInstant().get(), coreRaoRequest.getRealGlskFileUrl().get(), network))
                .thenReturn(glsks);

        Mockito.when(fileImporter.importVirtualHubs(coreRaoRequest.getVirtualhubsFileUrl().get())).thenReturn(virtualHubsConfiguration);
        Mockito.when(fileExporter.saveNetwork(network, coreRaoRequest)).thenReturn("simple-networkWithPRA-url");
        Mockito.when(fileExporter.saveRaoResult(raoResult, crac, coreRaoRequest, RaoParameters.load().getObjectiveFunctionParameters().getType().getUnit())).thenReturn("simple-RaoResultJson-url");

        RaoResponse raoResponse = raoRunnerService.runRao(coreRaoRequest);
        assertEquals("id", raoResponse.getId());
        assertEquals("http://host:9000/crac.json", raoResponse.getCracFileUrl());
        assertEquals("simple-networkWithPRA-url", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("simple-RaoResultJson-url", raoResponse.getRaoResultFileUrl());
        assertEquals(Optional.of("2019-01-08T12:30:00Z"), raoResponse.getInstant());
    }

    @Test
    void runRaoThrowsOpenRaoException() {
        RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("id")
                .withNetworkFileUrl("http://host:9000/network.xiidm")
                .withCracFileUrl("http://host:9000/crac.json")
                .withRaoParametersFileUrl("http://host:9000/raoParameters.json")
                .build();

        Mockito.when(fileImporter.importRaoParameters(simpleRaoRequest.getRaoParametersFileUrl())).thenReturn(new RaoParameters());
        Mockito.when(raoRunnerProv.run(Mockito.any(), Mockito.any())).thenThrow(new OpenRaoException("This is a test"));

        Assertions.assertThatThrownBy(() -> raoRunnerService.runRao(simpleRaoRequest))
                .isInstanceOf(RaoRunnerException.class)
                .hasCauseInstanceOf(OpenRaoException.class)
                .hasMessageContaining("This is a test");
    }
}
