/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    FileImporter fileImporter;

    @Test
    void checkIidmNetworkIsImportedCorrectly() throws FileImporterException {
        Network network = fileImporter.importNetwork(Objects.requireNonNull(getClass().getResource("/rao_inputs/network.xiidm")).toString());
        assertEquals("UCTE", network.getSourceFormat());
        assertEquals(4, network.getCountryCount());
    }

    @Test
    void importNetworkThrowsException() {
        Assertions.assertThatThrownBy(() -> fileImporter.importNetwork("http://networkUrl"))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing network networkUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'http://networkUrl' is not part of application's whitelisted url's");
    }

    @Test
    void checkJsonCracIsImportedCorrectly() throws FileImporterException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        Crac crac = fileImporter.importCrac(Objects.requireNonNull(getClass().getResource("/rao_inputs/crac.json")).toString(), network);
        assertEquals("rao test crac", crac.getId());
        assertEquals(1, crac.getContingencies().size());
        assertEquals(11, crac.getFlowCnecs().size());
    }

    @Test
    void importCracThrowsExceptionUrlFormat() {
        Assertions.assertThatThrownBy(() -> fileImporter.importCrac("cracUrl", null))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing CRAC file cracUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("Exception occurred while retrieving file name from cracUrl")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importCracThrowsExceptionWhitelist() {
        Assertions.assertThatThrownBy(() -> fileImporter.importCrac("http://cracUrl", null))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing CRAC file cracUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'http://cracUrl' is not part of application's whitelisted url's");
    }

    @Test
    void importCracThrowsExceptionContent() {
        Assertions.assertThatThrownBy(() -> fileImporter.importCrac("http://localhost:9000/cracUrl", null))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing CRAC file cracUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("Exception occurred while retrieving file content from http://localhost:9000/cracUrl")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void checkGlskIsImportedCorrectly() throws FileImporterException {
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
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Error occurred during GLSK Provider creation")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'glskUrl' is not part of application's whitelisted url's");
    }

    @Test
    void checkRefProgIsImportedCorrectly() throws FileImporterException {
        ReferenceProgram referenceProgram = fileImporter.importRefProg("2019-01-08T21:30:00Z",
                Objects.requireNonNull(getClass().getResource("/rao_inputs/refprog.xml")).toString());
        assertEquals(4, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(1600, referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8"));
    }

    @Test
    void importRefProgThrowsException() {
        Assertions.assertThatThrownBy(() -> fileImporter.importRefProg(null, "refprogUrl"))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Error occurred during Reference Program creation")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'refprogUrl' is not part of application's whitelisted url's");
    }

    @Test
    void importVirtualHubsThrowsException() {
        Assertions.assertThatThrownBy(() -> fileImporter.importVirtualHubs("virtualhubsUrl"))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Error occurred during virtualhubs Configuration creation")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'virtualhubsUrl' is not part of application's whitelisted url's");
    }

    @Test
    void importRaoParametersThrowsException() {
        Assertions.assertThatThrownBy(() -> fileImporter.importRaoParameters("raoParametersUrl"))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing rao parameters raoParametersUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'raoParametersUrl' is not part of application's whitelisted url's");
    }
}
