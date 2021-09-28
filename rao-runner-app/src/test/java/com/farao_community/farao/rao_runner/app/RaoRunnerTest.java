/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.farao_community.farao.search_tree_rao.SearchTreeRaoParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerTest {

    RaoRequest simpleRaoRequest = new RaoRequest("id", "networkFileUrl", "cracFileUrl");
    RaoRequest completeRaoRequest = new RaoRequest("id", "instant", "networkFileUrl", "cracFileUrl",
            "refprogFileUrl", "realGlskFileUrl", "file:/raoParametersFileUrl", "resultsDestination");

    @Autowired
    RaoRunnerServer raoRunnerServer;

    @MockBean
    MinioAdapter minioAdapter;

    @BeforeEach
    void setUp() throws IOException {

        InputStream networkInputStream = new ClassPathResource("/network.xiidm").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("networkFileUrl")).thenReturn(networkInputStream);
        Mockito.when(minioAdapter.getFileNameFromUrl("networkFileUrl")).thenReturn("network.xiidm");

        InputStream cracInputStream = new ClassPathResource("/crac.json").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("cracFileUrl")).thenReturn(cracInputStream);
        Mockito.when(minioAdapter.getFileNameFromUrl("cracFileUrl")).thenReturn("crac.json");

        Mockito.when(minioAdapter.getDefaultBasePath()).thenReturn("base-path");

        Mockito.when(minioAdapter.getFileNameFromUrl("file:/raoParametersFileUrl")).thenReturn("raoParametersFileUrl.json");
        InputStream raoParamsInputStream = new ClassPathResource("/raoParameters.json").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("file:/raoParametersFileUrl")).thenReturn(raoParamsInputStream);
    }

    @Test
    void checkIidmNetworkIsImportedCorrectly() {
        Network network = raoRunnerServer.importNetwork(simpleRaoRequest);
        assertEquals("UCTE", network.getSourceFormat());
        assertEquals(4, network.getCountryCount());
    }

    @Test
    void checkJsonCracIsImportedCorrectly() {
        Crac crac = raoRunnerServer.importCrac(simpleRaoRequest);
        assertEquals("rao test crac", crac.getId());
        assertEquals("N-1 NL1-NL3", crac.getContingencies().stream().findAny().get().getId());

    }

    @Test
    void checkGenerateResultsDestination() {
        String resultsDestination = raoRunnerServer.generateResultsDestination(simpleRaoRequest);
        assertEquals("base-path/id", resultsDestination);
    }

    @Test
    void checkDefaultRaoParametersAreImported() {
        RaoParameters defaultRaoParameters = raoRunnerServer.importRaoParameters(simpleRaoRequest);
        assertEquals(0, defaultRaoParameters.getLoopflowCountries().size());
        assertEquals(50.0, defaultRaoParameters.getMnecAcceptableMarginDiminution());
    }

    @Test
    void checkRequestedRaoParametersAreImported() {
        RaoParameters raoParameters = raoRunnerServer.importRaoParameters(completeRaoRequest);

        List<String> expectedLoopFlowConstraintCountries = Arrays.asList("AT", "BE", "CZ", "DE", "FR", "HR", "HU", "NL", "PL", "RO", "SI", "SK");
        List<String> actualLoopFlowConstraintCountries = raoParameters.getLoopflowCountries().stream().map(Country::toString).collect(Collectors.toList());
        assertTrue(expectedLoopFlowConstraintCountries.size() == actualLoopFlowConstraintCountries.size()
                && expectedLoopFlowConstraintCountries.containsAll(actualLoopFlowConstraintCountries)
                && actualLoopFlowConstraintCountries.containsAll(expectedLoopFlowConstraintCountries));
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        assertEquals(10.0, searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold());
        assertEquals(0, searchTreeRaoParameters.getMaxCurativeRaPerTso().get("AT"));
        assertEquals(3, searchTreeRaoParameters.getMaxCurativeRaPerTso().get("BE"));
        assertEquals(15, searchTreeRaoParameters.getMaxCurativeRaPerTso().size());
        assertEquals(0, searchTreeRaoParameters.getMaxCurativeTopoPerTso().get("AT"));
        assertEquals(1, searchTreeRaoParameters.getMaxCurativeTopoPerTso().get("BE"));
        assertEquals(2, searchTreeRaoParameters.getMaxCurativeTopoPerTso().get("CZ"));
        assertEquals(15, searchTreeRaoParameters.getMaxCurativeTopoPerTso().size());
        assertEquals(0, searchTreeRaoParameters.getMaxCurativePstPerTso().get("AT"));
        assertEquals(15, searchTreeRaoParameters.getMaxCurativePstPerTso().size());
    }
}
