/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterProperties;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
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
class FileExporterTest {

    @Autowired
    FileExporter fileExporter;
    @MockBean
    MinioAdapter minioAdapter;

    RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
            .withId("id")
            .withInstant("instant")
            .withNetworkFileUrl("networkFileUrl")
            .withCracFileUrl("cracFileUrl")
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .build();
    RaoRequest raoRequestWithResultDestination = new RaoRequest.RaoRequestBuilder()
            .withId("id")
            .withInstant("instant")
            .withNetworkFileUrl("networkFileUrl")
            .withCracFileUrl("cracFileUrl")
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .withResultsDestination("destination-key")
            .build();

    @BeforeEach
    public void setUp() {
        Mockito.when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "base/path", "http://test", "gridcapa", "gridcapa"));
        Mockito.doNothing().when(minioAdapter).uploadArtifact(Mockito.any(), Mockito.any());
    }

    @Test
    void checkRaoResultSavingWithResultDestination() {
        Mockito.when(minioAdapter.generatePreSignedUrl("destination-key/raoResult.json")).thenReturn("raoResultUrl");
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao_inputs/raoResult.json");
        Crac crac = CracImporters.importCrac("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")));
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        String resultsDestination = fileExporter.saveRaoResult(raoResult, crac, raoRequestWithResultDestination, Unit.AMPERE);
        assertEquals("raoResultUrl", resultsDestination);
    }

    @Test
    void checkRaoResultSavingWithNoResultDestination() {
        Mockito.when(minioAdapter.generatePreSignedUrl("base/path/id/raoResult.json")).thenReturn("raoResultUrl");
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao_inputs/raoResult.json");
        Crac crac = CracImporters.importCrac("crac.json", Objects.requireNonNull(getClass().getResourceAsStream("/rao_inputs/crac.json")));
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        String resultsDestination = fileExporter.saveRaoResult(raoResult, crac, simpleRaoRequest, Unit.AMPERE);
        assertEquals("raoResultUrl", resultsDestination);
    }

    @Test
    void checkNetworkSavingWithResultDestination() {
        Mockito.when(minioAdapter.generatePreSignedUrl("destination-key/networkWithPRA.xiidm")).thenReturn("networkWithPraUrl");

        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        String networkPraUrl = fileExporter.saveNetwork(network, raoRequestWithResultDestination);
        assertEquals("networkWithPraUrl", networkPraUrl);
    }

    @Test
    void checkNetworkSavingWithNoResultDestination() {
        Mockito.when(minioAdapter.generatePreSignedUrl("base/path/id/networkWithPRA.xiidm")).thenReturn("networkWithPraUrl");

        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
        String networkPraUrl = fileExporter.saveNetwork(network, simpleRaoRequest);
        assertEquals("networkWithPraUrl", networkPraUrl);
    }

}
