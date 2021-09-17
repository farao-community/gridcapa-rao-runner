/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.farao_community.farao.rao_runner.app.configuration.UrlWhitelistConfiguration;
import com.farao_community.farao.search_tree_rao.SearchTreeRaoParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Test
    void checkIidmNetworkIsImportedCorrectly() throws IOException {
        RaoRunnerServer raoRunnerServerMock = Mockito.mock(RaoRunnerServer.class);
        InputStream networkInputStream = new ClassPathResource("/network.xiidm").getInputStream();
        Mockito.when(raoRunnerServerMock.getInputStreamFromUrl("networkFileUrl")).thenReturn(networkInputStream);
        Mockito.when(raoRunnerServerMock.getFileNameFromUrl("networkFileUrl")).thenReturn("network.xiidm");
        Mockito.when(raoRunnerServerMock.importNetwork(simpleRaoRequest)).thenCallRealMethod();

        Network network = raoRunnerServerMock.importNetwork(simpleRaoRequest);
        assertEquals("UCTE", network.getSourceFormat());
        assertEquals(4, network.getCountryCount());
    }

    @Test
    void checkJsonCracIsImportedCorrectly() throws IOException {
        RaoRunnerServer raoRunnerServerMock = Mockito.mock(RaoRunnerServer.class);
        InputStream cracInputStream = new ClassPathResource("/crac.json").getInputStream();
        Mockito.when(raoRunnerServerMock.getInputStreamFromUrl("cracFileUrl")).thenReturn(cracInputStream);
        Mockito.when(raoRunnerServerMock.getFileNameFromUrl("cracFileUrl")).thenReturn("crac.json");
        Mockito.when(raoRunnerServerMock.importCrac(simpleRaoRequest)).thenCallRealMethod();

        Crac crac = raoRunnerServerMock.importCrac(simpleRaoRequest);
        assertEquals("rao test crac", crac.getId());
        assertEquals("N-1 NL1-NL3", crac.getContingencies().stream().findAny().get().getId());

    }

    @Test
    void checkGetFileNameFromUrl() {
        RaoRunnerServer raoRunnerServerMock = Mockito.mock(RaoRunnerServer.class);
        Mockito.when(raoRunnerServerMock.getFileNameFromUrl("http://host:9000/rao-integration-data/4/inputs/networks/network_fr.xiidm?X-Amz-Algo")).thenCallRealMethod();
        String fileName = raoRunnerServerMock.getFileNameFromUrl("http://host:9000/rao-integration-data/4/inputs/networks/network_fr.xiidm?X-Amz-Algo");
        assertEquals("network_fr.xiidm", fileName);
    }

    @Test
    void checkGenerateResultsDestination() {
        MinioAdapter minioAdapterMock = Mockito.mock(MinioAdapter.class);
        Mockito.when(minioAdapterMock.getDefaultBasePath()).thenReturn("base-path");
        String resultsDestination = raoRunnerServer.generateResultsDestination(simpleRaoRequest);
        assertEquals("base/path/id", resultsDestination);
    }

    @Test
    void checkExceptionThrown() {
        UrlWhitelistConfiguration urlWhitelistConfigurationMock = Mockito.mock(UrlWhitelistConfiguration.class);
        Mockito.when(urlWhitelistConfigurationMock.getWhitelist()).thenReturn(Arrays.asList("url1", "url2"));
        Exception exception = assertThrows(RaoRunnerException.class, () -> {
            raoRunnerServer.getInputStreamFromUrl("notWhiteListedUrl");
        });
        String expectedMessage = "is not part of application's whitelisted url's";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void checkDefaultRaoParametersAreImported() {
        RaoParameters defaultRaoParameters = raoRunnerServer.importRaoParameters(simpleRaoRequest);
        assertEquals(0, defaultRaoParameters.getLoopflowCountries().size());
        assertEquals(50.0, defaultRaoParameters.getMnecAcceptableMarginDiminution());
    }

    @Test
    void checkRequestedRaoParametersAreImported() throws IOException {
        RaoRunnerServer raoRunnerServerMock = Mockito.mock(RaoRunnerServer.class);
        Mockito.when(raoRunnerServerMock.getFileNameFromUrl("file:/raoParametersFileUrl")).thenReturn("raoParametersFileUrl.json");
        InputStream raoParamsInputStream = new ClassPathResource("/raoParameters.json").getInputStream();
        Mockito.when(raoRunnerServerMock.getInputStreamFromUrl("file:/raoParametersFileUrl")).thenReturn(raoParamsInputStream);
        Mockito.when(raoRunnerServerMock.importRaoParameters(completeRaoRequest)).thenCallRealMethod();
        RaoParameters raoParameters = raoRunnerServerMock.importRaoParameters(completeRaoRequest);

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
