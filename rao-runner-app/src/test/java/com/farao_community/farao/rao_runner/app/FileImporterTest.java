/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Test
    void checkIidmNetworkIsImportedCorrectly() {
        Network network = fileImporter.importNetwork(Objects.requireNonNull(getClass().getResource("/rao_inputs/network.xiidm")).toString());
        assertEquals("UCTE", network.getSourceFormat());
        assertEquals(4, network.getCountryCount());
    }

    @Test
    void importNetworkThrowsException() {

        Assertions.assertThatThrownBy(() -> fileImporter.importNetwork("networkUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(MalformedURLException.class)
            .hasMessageContaining("Exception occurred while retrieving file name from : networkUrl")
            .getCause()
            .hasMessageContaining("no protocol: networkUrl");
    }

    @Test
    void checkJsonCracIsImportedCorrectly() {
        Crac crac = fileImporter.importCrac(Objects.requireNonNull(getClass().getResource("/rao_inputs/crac.json")).toString());
        assertEquals("rao test crac", crac.getId());
        assertEquals(1, crac.getContingencies().size());
        assertEquals(11, crac.getFlowCnecs().size());
    }

    @Test
    void importCracThrowsException() {

        Assertions.assertThatThrownBy(() -> fileImporter.importCrac("cracUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(MalformedURLException.class)
            .hasMessageContaining("Exception occurred while retrieving file name from : cracUrl")
            .getCause()
            .hasMessageContaining("no protocol: cracUrl");
    }

    @Test
    void checkGlskIsImportedCorrectly() {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        ZonalData<SensitivityVariableSet> glsks = fileImporter.importGlsk("2019-01-08T21:30:00Z",
                Objects.requireNonNull(getClass().getResource("/rao_inputs/glsk.xml")).toString(),
                network);
        assertEquals(4, glsks.getDataPerZone().size());
        assertEquals(3, glsks.getData("10YFR-RTE------C").getVariables().size());

    }

    @Test
    void importGlskThrowsException() {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        Assertions.assertThatThrownBy(() -> fileImporter.importGlsk(null, "glskUrl", network))
                .isInstanceOf(RaoRunnerException.class)
                .hasCauseInstanceOf(MalformedURLException.class)
                .hasMessageContaining("Exception occurred while retrieving file name from : glskUrl")
                .getCause()
                .hasMessageContaining("no protocol: glskUrl");
    }

    @Test
    void checkRefProgIsImportedCorrectly() {
        ReferenceProgram referenceProgram = fileImporter.importRefProg("2019-01-08T21:30:00Z",
                Objects.requireNonNull(getClass().getResource("/rao_inputs/refprog.xml")).toString());
        assertEquals(4, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(1600, referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8"));
    }

    @Test
    void importRefProgThrowsException() {
        Assertions.assertThatThrownBy(() -> fileImporter.importRefProg(null, "refprogUrl"))
                .isInstanceOf(RaoRunnerException.class)
                .hasCauseInstanceOf(MalformedURLException.class)
                .hasMessageContaining("Exception occurred while retrieving file name from : refprogUrl")
                .getCause()
                .hasMessageContaining("no protocol: refprogUrl");
    }

    @Test
    void importVirtualHubsThrowsException() {

        Assertions.assertThatThrownBy(() -> fileImporter.importVirtualHubs("virtualhubsUrl"))
            .isInstanceOf(RaoRunnerException.class)
            .hasCauseInstanceOf(MalformedURLException.class)
            .hasMessageContaining("Exception occurred while retrieving file name from : virtualhubsUrl")
            .getCause()
            .hasMessageContaining("no protocol: virtualhubsUrl");
    }

    @Test
    void checkRaoParametersImport() {
        RaoParameters raoParameters = fileImporter.importRaoParameters(Objects.requireNonNull(getClass().getResource("/rao_inputs/raoParameters.json")).toString());

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
    }
}
