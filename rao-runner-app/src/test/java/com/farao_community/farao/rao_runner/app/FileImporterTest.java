/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.rte_france.powsybl.iidm.export.adn.ADNLoadFlowParameters;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    FileImporter fileImporter;
    @MockBean
    MinioAdapter minioAdapter;

    @BeforeEach
    void setUp() throws IOException {
        InputStream networkInputStream = new ClassPathResource("/rao_inputs/network.xiidm").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("networkFileUrl")).thenReturn(networkInputStream);
        Mockito.when(minioAdapter.getFileNameFromUrl("networkFileUrl")).thenReturn("network.xiidm");

        InputStream cracInputStream = new ClassPathResource("/rao_inputs/crac.json").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("cracFileUrl")).thenReturn(cracInputStream);
        Mockito.when(minioAdapter.getFileNameFromUrl("cracFileUrl")).thenReturn("crac.json");

        InputStream glskInputStream = new ClassPathResource("/rao_inputs/glsk.xml").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("glskFileUrl")).thenReturn(glskInputStream);
        Mockito.when(minioAdapter.getFileNameFromUrl("glskFileUrl")).thenReturn("glsk.xml");

        InputStream refProgInputStream = new ClassPathResource("/rao_inputs/refprog.xml").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("refprogFileUrl")).thenReturn(refProgInputStream);
        Mockito.when(minioAdapter.getFileNameFromUrl("refprogFileUrl")).thenReturn("refprog.xml");

        Mockito.when(minioAdapter.getDefaultBasePath()).thenReturn("base-path");
        Mockito.when(minioAdapter.getFileNameFromUrl("raoParametersAdnLoadflowFileUrl")).thenReturn("raoParametersFileUrl.json");
        InputStream raoParamsAdnLfInputStream = new ClassPathResource("/rao_inputs/raoParametersWithAdnLoadflow.json").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("raoParametersAdnLoadflowFileUrl")).thenReturn(raoParamsAdnLfInputStream);
    }

    @Test
    void checkIidmNetworkIsImportedCorrectly() {
        Network network = fileImporter.importNetwork("networkFileUrl");
        assertEquals("UCTE", network.getSourceFormat());
        assertEquals(4, network.getCountryCount());
    }

    @Test
    void importNetworkThrowsException() {
        Mockito.when(minioAdapter.getInputStreamFromUrl("networkUrl")).thenThrow(new RaoRunnerException("This is a test"));
        Mockito.when(minioAdapter.getFileNameFromUrl("networkUrl")).thenReturn("test");
        Assertions.assertThatThrownBy(() -> fileImporter.importNetwork("networkUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(RaoRunnerException.class)
            .hasMessageContaining("Exception occurred while importing network")

            .getCause()
            .hasMessageContaining("This is a test");
    }

    @Test
    void checkJsonCracIsImportedCorrectly() {
        Crac crac = fileImporter.importCrac("cracFileUrl");
        assertEquals("rao test crac", crac.getId());
        assertEquals(1, crac.getContingencies().size());
        assertEquals(11, crac.getFlowCnecs().size());
    }

    @Test
    void importCracThrowsException() {
        Mockito.when(minioAdapter.getInputStreamFromUrl("cracUrl")).thenThrow(new RaoRunnerException("This is a test"));
        Mockito.when(minioAdapter.getFileNameFromUrl("cracUrl")).thenReturn("test");
        Assertions.assertThatThrownBy(() -> fileImporter.importCrac("cracUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(RaoRunnerException.class)
            .hasMessageContaining("Exception occurred while importing CRAC file")

            .getCause()
            .hasMessageContaining("This is a test");
    }

    @Test
    void checkGlskIsImportedCorrectly() {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        ZonalData<SensitivityVariableSet> glsks = fileImporter.importGlsk("2019-01-08T21:30:00Z", "glskFileUrl", network);
        assertEquals(4, glsks.getDataPerZone().size());
        assertEquals(3, glsks.getData("10YFR-RTE------C").getVariables().size());

    }

    @Test
    void importGlskThrowsException() {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        Mockito.when(minioAdapter.getInputStreamFromUrl("glskUrl")).thenThrow(new RaoRunnerException("This is a test"));
        Mockito.when(minioAdapter.getFileNameFromUrl("glskUrl")).thenReturn("test");
        Assertions.assertThatThrownBy(() -> fileImporter.importGlsk(null, "glskUrl", network))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(RaoRunnerException.class)
            .hasMessageContaining("Error occurred during GLSK Provider creation for timestamp")

            .getCause()
            .hasMessageContaining("This is a test");
    }

    @Test
    void checkRefProgIsImportedCorrectly() {
        ReferenceProgram referenceProgram = fileImporter.importRefProg("2019-01-08T21:30:00Z", "refprogFileUrl");
        assertEquals(4, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(1600, referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8"));
    }

    @Test
    void importRefProgThrowsException() {
        Mockito.when(minioAdapter.getInputStreamFromUrl("refprogUrl")).thenThrow(new RaoRunnerException("This is a test"));
        Mockito.when(minioAdapter.getFileNameFromUrl("refprogUrl")).thenReturn("test");
        Assertions.assertThatThrownBy(() -> fileImporter.importRefProg(null, "refprogUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(RaoRunnerException.class)
            .hasMessageContaining("Error occurred during Reference Program creation for timestamp")

            .getCause()
            .hasMessageContaining("This is a test");
    }

    @Test
    void importVirtualHubsThrowsException() {
        Mockito.when(minioAdapter.getInputStreamFromUrl("virtualhubsUrl")).thenThrow(new RaoRunnerException("This is a test"));
        Mockito.when(minioAdapter.getFileNameFromUrl("virtualhubsUrl")).thenReturn("test");
        Assertions.assertThatThrownBy(() -> fileImporter.importVirtualHubs("virtualhubsUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(RaoRunnerException.class)
            .hasMessageContaining("Error occurred during virtualhubs Configuration creation using virtualhubs file")

            .getCause()
            .hasMessageContaining("This is a test");
    }

    @Test
    void checkParametersImportWithAdnLoadflow() {
        RaoParameters raoParameters = fileImporter.importRaoParameters("raoParametersAdnLoadflowFileUrl");

        List<String> expectedLoopFlowConstraintCountries = Arrays.asList("AT", "BE", "CZ", "DE", "FR", "HR", "HU", "NL", "PL", "RO", "SI", "SK");
        LoopFlowParametersExtension loopFlowParametersExtension = raoParameters.getExtension(LoopFlowParametersExtension.class);
        List<String> actualLoopFlowConstraintCountries = loopFlowParametersExtension.getCountries().stream().map(Country::toString).collect(Collectors.toList());
        assertTrue(expectedLoopFlowConstraintCountries.size() == actualLoopFlowConstraintCountries.size()
                && expectedLoopFlowConstraintCountries.containsAll(actualLoopFlowConstraintCountries)
                && actualLoopFlowConstraintCountries.containsAll(expectedLoopFlowConstraintCountries));
        Map<String, Integer> maxCurativeRaPerTso = raoParameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRaPerTso();
        Map<String, Integer>  getMaxCurativeTopoPerTso = raoParameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeTopoPerTso();
        Map<String, Integer>  getMaxCurativePstPerTso = raoParameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativePstPerTso();

        assertEquals(10.0, raoParameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());
        assertEquals(0, maxCurativeRaPerTso.get("AT"));
        assertEquals(3, maxCurativeRaPerTso.get("BE"));
        assertEquals(15, maxCurativeRaPerTso.size());
        assertEquals(0, getMaxCurativeTopoPerTso.get("AT"));
        assertEquals(1, getMaxCurativeTopoPerTso.get("BE"));
        assertEquals(2, getMaxCurativeTopoPerTso.get("CZ"));
        assertEquals(15, getMaxCurativeTopoPerTso.size());
        assertEquals(0, getMaxCurativePstPerTso.get("AT"));
        assertEquals(15, getMaxCurativePstPerTso.size());

        ADNLoadFlowParameters adnLoadFlowParameters = raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().getExtension(ADNLoadFlowParameters.class);
        assertEquals(ADNLoadFlowParameters.TypeApproxActifSeul.COURANT_CONTINU, adnLoadFlowParameters.getDcApproxType());
        assertEquals(1.0, adnLoadFlowParameters.getDcCosphi(), .0);
        assertEquals(2, adnLoadFlowParameters.getNbThreads());
    }
}
