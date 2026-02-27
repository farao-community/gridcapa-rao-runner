/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterProperties;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @Autowired
    private TimeCoupledRaoRunnerService timeCoupledRaoRunnerService;
    @Autowired
    private FileExporter fileExporter;
    @MockitoBean
    private MinioAdapter minioAdapter;

    private final RaoRequest simpleRaoRequest = new RaoRequest.RaoRequestBuilder()
            .withId("id")
            .withInstant("instant")
            .withNetworkFileUrl("networkFileUrl")
            .withCracFileUrl("cracFileUrl")
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .build();
    private final RaoRequest raoRequestWithResultDestination = new RaoRequest.RaoRequestBuilder()
            .withId("id")
            .withInstant("instant")
            .withNetworkFileUrl("networkFileUrl")
            .withCracFileUrl("cracFileUrl")
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .withResultsDestination("destination-key")
            .build();
    private final Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));

    @BeforeEach
    void setUp() {
        Mockito.when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "base/path", "http://test", "gridcapa", "gridcapa"));
        Mockito.doNothing().when(minioAdapter).uploadArtifact(Mockito.any(), Mockito.any());
    }

    // RaoRunner Network

    @Test
    void saveNetworkWithResultDestinationTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl("destination-key/networkWithPRA.xiidm")).thenReturn("networkWithPraUrl");

        final String networkPraUrl = fileExporter.saveNetwork(network, raoRequestWithResultDestination);

        Assertions.assertThat(networkPraUrl).isEqualTo("networkWithPraUrl");
    }

    @Test
    void saveNetworkWithNoResultDestinationTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl("base/path/id/networkWithPRA.xiidm")).thenReturn("networkWithPraUrl");

        final String networkPraUrl = fileExporter.saveNetwork(network, simpleRaoRequest);

        Assertions.assertThat(networkPraUrl).isEqualTo("networkWithPraUrl");
    }

    // TODO Add tests for saveNetworks()

    @Test
    void checkRaoResultSavingWithResultDestination() throws IOException {
        final Crac crac = Crac.read("crac.json", getResourceAsStream("/rao_inputs/crac.json"), network);
        final RaoResult raoResult = RaoResult.read(getResourceAsStream("/rao_inputs/raoResult.json"), crac);
        Mockito.when(minioAdapter.generatePreSignedUrl("destination-key/raoResult.json")).thenReturn("raoResultUrl");

        final String resultsDestination = fileExporter.saveRaoResult(raoResult, crac, raoRequestWithResultDestination, Unit.AMPERE);

        Assertions.assertThat(resultsDestination).isEqualTo("raoResultUrl");
    }

    @Test
    void checkRaoResultSavingWithNoResultDestination() throws IOException {
        final Crac crac = Crac.read("crac.json", getResourceAsStream("/rao_inputs/crac.json"), network);
        final RaoResult raoResult = RaoResult.read(getResourceAsStream("/rao_inputs/raoResult.json"), crac);
        Mockito.when(minioAdapter.generatePreSignedUrl("base/path/id/raoResult.json")).thenReturn("raoResultUrl");

        final String resultsDestination = fileExporter.saveRaoResult(raoResult, crac, simpleRaoRequest, Unit.AMPERE);

        Assertions.assertThat(resultsDestination).isEqualTo("raoResultUrl");
    }

    // TODO Add tests for saveTimeCoupledRaoResult()

    private InputStream getResourceAsStream(final String resourcePath) {
        return Objects.requireNonNull(getClass().getResourceAsStream(resourcePath));
    }
}
