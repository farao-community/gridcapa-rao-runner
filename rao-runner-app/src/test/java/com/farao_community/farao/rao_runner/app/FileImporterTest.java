/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.app.exceptions.FileImporterException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    FileImporter fileImporter;

    private String getResourcePath(final String resourceRelativePath) {
        return Objects.requireNonNull(getClass().getResource(resourceRelativePath)).toString();
    }

    @Test
    void checkRaoParametersIsImportedCorrectly() throws FileImporterException {
        final RaoParameters raoParameters = fileImporter.importRaoParameters(getResourcePath("/timecoupled_rao_inputs/simple_case/raoParameters.json"));
        Assertions.assertThat(raoParameters.getObjectiveFunctionParameters().getType()).isEqualTo(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
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

    @Test
    void checkIidmNetworkIsImportedCorrectly() throws FileImporterException {
        final Network network = fileImporter.importNetwork(getResourcePath("/rao_inputs/network.xiidm"));
        Assertions.assertThat(network.getSourceFormat()).isEqualTo("UCTE");
        Assertions.assertThat(network.getCountryCount()).isEqualTo(4);
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
        final Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        final Crac crac = fileImporter.importCrac(getResourcePath("/rao_inputs/crac.json"), network);
        Assertions.assertThat(crac.getId()).isEqualTo("rao test crac");
        Assertions.assertThat(crac.getContingencies()).hasSize(1);
        Assertions.assertThat(crac.getFlowCnecs()).hasSize(11);
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
    void checkJsonCracIsImportedCorrectlyWithContext() throws FileImporterException {
        final Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        final Crac crac = fileImporter.importCracWithContext(getResourcePath("/rao_inputs/crac.json"), network);
        Assertions.assertThat(crac.getId()).isEqualTo("rao test crac");
        Assertions.assertThat(crac.getContingencies()).hasSize(1);
        Assertions.assertThat(crac.getFlowCnecs()).hasSize(11);
    }

    @Test
    void importCracWithContextThrowsExceptionUrlFormat() {
        Assertions.assertThatThrownBy(() -> fileImporter.importCracWithContext("cracUrl", null))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing CRAC file cracUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("Exception occurred while retrieving file name from cracUrl")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importCracWithContextThrowsExceptionWhitelist() {
        Assertions.assertThatThrownBy(() -> fileImporter.importCracWithContext("http://cracUrl", null))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing CRAC file cracUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'http://cracUrl' is not part of application's whitelisted url's");
    }

    @Test
    void importCracWithContextThrowsExceptionContent() {
        Assertions.assertThatThrownBy(() -> fileImporter.importCracWithContext("http://localhost:9000/cracUrl", null))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Exception occurred while importing CRAC file cracUrl")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("Exception occurred while retrieving file content from http://localhost:9000/cracUrl")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void checkGlskIsImportedCorrectly() throws FileImporterException {
        final Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        final ZonalData<SensitivityVariableSet> glsks = fileImporter.importGlsk("2019-01-08T21:30:00Z",
                getResourcePath("/rao_inputs/glsk.xml"),
                network);
        Assertions.assertThat(glsks.getDataPerZone()).hasSize(4);
        Assertions.assertThat(glsks.getData("10YFR-RTE------C").getVariables()).hasSize(3);
    }

    @Test
    void importGlskThrowsException() {
        final Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        Assertions.assertThatThrownBy(() -> fileImporter.importGlsk(null, "glskUrl", network))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Error occurred during GLSK Provider creation")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'glskUrl' is not part of application's whitelisted url's");
    }

    @Test
    void checkRefProgIsImportedCorrectly() throws FileImporterException {
        final ReferenceProgram referenceProgram = fileImporter.importRefProg("2019-01-08T21:30:00Z",
                getResourcePath("/rao_inputs/refprog.xml"));
        Assertions.assertThat(referenceProgram.getReferenceExchangeDataList()).hasSize(4);
        Assertions.assertThat(referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8")).isEqualTo(1600);
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
    void checkVirtualHubsIsImportedCorrectly() throws FileImporterException {
        final VirtualHubsConfiguration virtualHubsConfiguration = fileImporter.importVirtualHubs(getResourcePath("/rao_inputs/virtualHubsConfigurationFile.xml"));
        Assertions.assertThat(virtualHubsConfiguration.getBorderDirections()).isEmpty();
        Assertions.assertThat(virtualHubsConfiguration.getMarketAreas()).hasSize(3);
        Assertions.assertThat(virtualHubsConfiguration.getVirtualHubs()).hasSize(4);
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
    void checkIcsIsImportedCorrectly() throws FileImporterException {
        final TimeCoupledConstraints timeCoupledConstraints = fileImporter.importIcsFile(getResourcePath("/timecoupled_rao_inputs/simple_case/timeCoupledConstraints.json"));
        Assertions.assertThat(timeCoupledConstraints.getGeneratorConstraints()).hasSize(2);
    }

    @Test
    void importIcsThrowsException() {
        Assertions.assertThatThrownBy(() -> fileImporter.importIcsFile("icsUrl"))
                .isInstanceOf(FileImporterException.class)
                .hasMessageContaining("Error occurred while reading ICS file")
                .hasCauseInstanceOf(RaoRunnerException.class)
                .cause()
                .hasMessageContaining("URL 'icsUrl' is not part of application's whitelisted url's");
    }
}
