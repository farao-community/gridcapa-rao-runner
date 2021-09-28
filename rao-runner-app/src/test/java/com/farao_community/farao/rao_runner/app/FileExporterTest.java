/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
public class FileExporterTest {

    @Autowired
    FileExporter fileExporter;
    @MockBean
    MinioAdapter minioAdapter;
    RaoRequest simpleRaoRequest = new RaoRequest("id", "networkFileUrl", "cracFileUrl");

    @BeforeEach
    public void setUp() {
        Mockito.when(minioAdapter.getFileNameFromUrl(Mockito.any())).thenCallRealMethod();
        Mockito.when(minioAdapter.getDefaultBasePath()).thenReturn("base/path");
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any());
        Mockito.when(minioAdapter.generatePreSignedUrl("destination-key/raoResult.json")).thenReturn("raoResultUrl");
        Mockito.when(minioAdapter.generatePreSignedUrl("destination-key/networkWithPRA.xiidm")).thenReturn("networkWithPraUrl");
    }

    @Test
    void checkGenerateResultsDestination() {
        String resultsDestination = fileExporter.generateResultsDestination(simpleRaoRequest);
        assertEquals("base/path/id", resultsDestination);
    }

    @Test
    void checkRaoResultCreation() {
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao_inputs/raoResult.json");
        Crac crac = CracImporters.importCrac("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")));
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        String resultsDestination = fileExporter.exportAndSaveJsonRaoResult(raoResult, crac, "destination-key");
        assertEquals("raoResultUrl", resultsDestination);
    }

    @Test
    void checkNetworkWithCreation() {
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao_inputs/raoResult.json");
        Network network = Importers.loadNetwork("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        Crac crac = CracImporters.importCrac("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")));
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        String resultsDestination = fileExporter.exportAndSaveNetworkWithPra(raoResult, network, "destination-key");
        assertEquals("networkWithPraUrl", resultsDestination);
    }

}
