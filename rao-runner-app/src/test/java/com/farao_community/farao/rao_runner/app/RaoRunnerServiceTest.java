/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
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

    Network network;
    Crac crac;
    RaoResult raoResult;
    ZonalData<LinearGlsk> glsks;
    ReferenceProgram referenceProgram;

    @BeforeEach
    public void setUp() {
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao_inputs/raoResult.json");
        network = Importers.loadNetwork("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        crac = CracImporters.importCrac("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")));
        raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        Mockito.when(raoRunnerProv.run(Mockito.any(), Mockito.any())).thenReturn(raoResult);

        InputStream glskFileInputStream = getClass().getResourceAsStream("/rao_inputs/glsk.xml");
        GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(Objects.requireNonNull(glskFileInputStream));
        InputStream refProgFileInputStream = getClass().getResourceAsStream("/rao_inputs/refprog.xml");

        glsks = ucteGlskProvider.getZonalGlsks(network, OffsetDateTime.parse("2019-01-08T12:30:00Z").toInstant());
        referenceProgram = RefProgImporter.importRefProg(refProgFileInputStream, OffsetDateTime.parse("2019-01-08T12:30:00Z"));

        Mockito.when(fileImporter.importNetwork(Mockito.any())).thenReturn(network);
        Mockito.when(fileImporter.importCrac(Mockito.any())).thenReturn(crac);
    }

    @Test
    void checkSimpleRaoRun() {
        RaoRequest simpleRaoRequest = new RaoRequest("id", "http://host:9000/network.xiidm", "http://host:9000/crac.json");

        Mockito.when(fileExporter.saveNetwork(network, simpleRaoRequest)).thenReturn("simple-networkWithPRA-url");
        Mockito.when(fileExporter.saveRaoResult(raoResult, crac, simpleRaoRequest)).thenReturn("simple-RaoResultJson-url");

        RaoResponse raoResponse = raoRunnerService.runRao(simpleRaoRequest);
        assertEquals("id", raoResponse.getId());
        assertEquals("http://host:9000/crac.json", raoResponse.getCracFileUrl());
        assertEquals("simple-networkWithPRA-url", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("simple-RaoResultJson-url", raoResponse.getRaoResultFileUrl());
        assertEquals(Optional.empty(), raoResponse.getInstant());
    }

    @Test
    void checkCoreRaoRun() {
        RaoRequest coreRaoRequest = new RaoRequest("id",
                "2019-01-08T12:30:00Z",
                "http://host:9000/network.xiidm",
                "http://host:9000/crac.json",
                "http://host:9000/refProg.xml",
                "http://host:9000/glsk.xml",
                "raoParams.json",
                "destination-key");

        Mockito.when(fileExporter.saveNetwork(network, coreRaoRequest)).thenReturn("simple-networkWithPRA-url");
        Mockito.when(fileExporter.saveRaoResult(raoResult, crac, coreRaoRequest)).thenReturn("simple-RaoResultJson-url");

        RaoResponse raoResponse = raoRunnerService.runRao(coreRaoRequest, network, crac, Optional.of(glsks), Optional.of(referenceProgram), new RaoParameters());
        assertEquals("id", raoResponse.getId());
        assertEquals("http://host:9000/crac.json", raoResponse.getCracFileUrl());
        assertEquals("simple-networkWithPRA-url", raoResponse.getNetworkWithPraFileUrl());
        assertEquals("simple-RaoResultJson-url", raoResponse.getRaoResultFileUrl());
        assertEquals(Optional.of("2019-01-08T12:30:00Z"), raoResponse.getInstant());
    }
}
