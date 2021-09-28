/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.farao_community.farao.search_tree_rao.SearchTreeRaoParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
public class FileImporterTest {

    @Autowired
    FileImporter fileImporter;
    RaoRequest raoRequest = new RaoRequest("id", "2019-01-08T21:30:00Z", "networkFileUrl", "cracFileUrl",
            "refprogFileUrl", "glskFileUrl", "file:/raoParametersFileUrl", "resultsDestination");
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
        Mockito.when(minioAdapter.getFileNameFromUrl("file:/raoParametersFileUrl")).thenReturn("raoParametersFileUrl.json");
        InputStream raoParamsInputStream = new ClassPathResource("/rao_inputs/raoParameters.json").getInputStream();
        Mockito.when(minioAdapter.getInputStreamFromUrl("file:/raoParametersFileUrl")).thenReturn(raoParamsInputStream);
    }

    @Test
    void checkIidmNetworkIsImportedCorrectly() {
        Network network = fileImporter.importNetwork(raoRequest);
        assertEquals("UCTE", network.getSourceFormat());
        assertEquals(4, network.getCountryCount());
    }

    @Test
    void checkJsonCracIsImportedCorrectly() {
        Crac crac = fileImporter.importCrac(raoRequest);
        assertEquals("rao test crac", crac.getId());
        assertEquals(1, crac.getContingencies().size());
        assertEquals(11, crac.getFlowCnecs().size());
    }

    @Test
    void checkGlskIsImportedCorrectly() {
        Network network = Importers.loadNetwork("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        ZonalData<LinearGlsk> glsks = fileImporter.importGlsk(raoRequest, network).get();
        assertEquals(4, glsks.getDataPerZone().size());
        assertEquals(3, glsks.getData("10YFR-RTE------C").getGLSKs().size());

    }

    @Test
    void checkRefProgIsImportedCorrectly() {
        ReferenceProgram referenceProgram = fileImporter.importRefProg(raoRequest).get();
        assertEquals(4, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(1600, referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8"));
    }

    @Test
    void checkDefaultRaoParametersAreImported() {
        RaoRequest simpleRaoRequest = new RaoRequest("id", "networkUrl", "cracUrl");
        RaoParameters defaultRaoParameters = fileImporter.importRaoParameters(simpleRaoRequest);
        assertEquals(0, defaultRaoParameters.getLoopflowCountries().size());
        assertEquals(50.0, defaultRaoParameters.getMnecAcceptableMarginDiminution());
    }

    @Test
    void checkRequestedRaoParametersAreImported() {
        RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest);

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
