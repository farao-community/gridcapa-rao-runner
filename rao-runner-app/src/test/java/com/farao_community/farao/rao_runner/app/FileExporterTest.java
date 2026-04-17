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
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimedInput;
import com.farao_community.farao.rao_runner.app.exceptions.FileExporterException;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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

    private final Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/rao_inputs/network.xiidm"));
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

    private TimeCoupledRaoRequest getSimpleTimeCoupledRaoRequest(final String networkFileExt) {
        return new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("id")
            .withIcsFileUrl("icsFileUrl")
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .withTimedInputs(List.of(new TimedInput(OffsetDateTime.parse("2026-04-16T10:30Z"), "networkFileUrl" + networkFileExt, "cracFileUrl")))
            .build();
    }

    private TimeCoupledRaoRequest getTimeCoupledRaoRequestWithResultDestination(final String networkFileExt) {
        return new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("id")
            .withIcsFileUrl("icsFileUrl")
            .withRaoParametersFileUrl("raoParametersFileUrl")
            .withTimedInputs(List.of(new TimedInput(OffsetDateTime.parse("2026-04-16T10:30Z"), "networkFileUrl" + networkFileExt, "cracFileUrl")))
            .withResultsDestination("destination-key")
            .build();
    }

    @BeforeEach
    void setUp() {
        Mockito.when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "base/path", "http://test", "gridcapa", "gridcapa"));
        Mockito.doNothing().when(minioAdapter).uploadArtifact(Mockito.any(), Mockito.any());
    }

    private void checkNetworkSaving(final String filePath, final RaoRequest raoRequest) {
        Mockito.when(minioAdapter.generatePreSignedUrl(filePath)).thenReturn("networkWithPraUrl");

        final String networkPraUrl = fileExporter.saveNetwork(network, raoRequest);

        Assertions.assertThat(networkPraUrl).isEqualTo("networkWithPraUrl");
    }

    @Test
    void saveNetworkWithResultDestinationTest() {
        checkNetworkSaving("destination-key/networkWithPRA.xiidm", raoRequestWithResultDestination);
    }

    @Test
    void saveNetworkWithNoResultDestinationTest() {
        checkNetworkSaving("base/path/id/networkWithPRA.xiidm", simpleRaoRequest);
    }

    private void checkTimeCoupledNetworkSaving(final String filePath, final TimeCoupledRaoRequest raoRequest) throws FileExporterException, IOException {
        final OffsetDateTime timestamp = OffsetDateTime.parse("2026-04-16T10:30Z");
        final Crac crac = Crac.read("crac.json", getResourceAsStream("/rao_inputs/crac.json"), network);
        final TemporalDataImpl<RaoInput> raoInputs = new TemporalDataImpl<>();
        raoInputs.put(timestamp, RaoInput.build(network, crac).build());
        Mockito.when(minioAdapter.generatePreSignedUrl(filePath)).thenReturn("networksWithPraUrl");

        final String networkPraUrl = fileExporter.saveNetworks(Map.of(timestamp, network), raoRequest);

        Assertions.assertThat(networkPraUrl).isEqualTo("networksWithPraUrl");
    }

    @ParameterizedTest
    @ValueSource(strings = {".biidm", ".jiidm", ".xiidm"})
    void saveTimeCoupledNetworkWithResultDestinationTest(final String networkFileExtension) throws FileExporterException, IOException {
        final TimeCoupledRaoRequest timeCoupledRaoRequest = getTimeCoupledRaoRequestWithResultDestination(networkFileExtension);
        checkTimeCoupledNetworkSaving("destination-key/networksWithPRA.zip", timeCoupledRaoRequest);
    }

    @ParameterizedTest
    @ValueSource(strings = {".biidm", ".jiidm", ".xiidm"})
    void saveTimeCoupledNetworkWithNoResultDestinationTest(final String networkFileExtension) throws FileExporterException, IOException {
        final TimeCoupledRaoRequest timeCoupledRaoRequest = getSimpleTimeCoupledRaoRequest(networkFileExtension);
        checkTimeCoupledNetworkSaving("base/path/id/networksWithPRA.zip", timeCoupledRaoRequest);
    }

    @Test
    void saveTimeCoupledNetworkWithInvalidFormatThrowsTest() {
        final TimeCoupledRaoRequest timeCoupledRaoRequest = getTimeCoupledRaoRequestWithResultDestination(".txt");
        Assertions.assertThatExceptionOfType(FileExporterException.class)
            .isThrownBy(() -> checkTimeCoupledNetworkSaving("base/path/id/networksWithPRA.zip", timeCoupledRaoRequest))
            .withMessage("Unsupported network format \"txt\" with filename networkFileUrl.txt");
    }

    private void checkRaoResultSaving(final String filePath, final RaoRequest raoRequestWithResultDestination) throws IOException {
        final Crac crac = Crac.read("crac.json", getResourceAsStream("/rao_inputs/crac.json"), network);
        final RaoResult raoResult = RaoResult.read(getResourceAsStream("/rao_inputs/raoResult.json"), crac);
        Mockito.when(minioAdapter.generatePreSignedUrl(filePath)).thenReturn("raoResultUrl");

        final String resultsDestination = fileExporter.saveRaoResult(raoResult, crac, raoRequestWithResultDestination, Unit.AMPERE);

        Assertions.assertThat(resultsDestination).isEqualTo("raoResultUrl");
    }

    @Test
    void saveRaoResultWithResultDestinationTest() throws IOException {
        checkRaoResultSaving("destination-key/raoResult.json", raoRequestWithResultDestination);
    }

    @Test
    void saveRaoResultWithoutResultDestinationTest() throws IOException {
        checkRaoResultSaving("base/path/id/raoResult.json", simpleRaoRequest);
    }

    private void checkTimeCoupledRaoResultSaving(final String filePath, final TimeCoupledRaoRequest timeCoupledRaoRequestWithResultDestination) throws FileExporterException, IOException {
        final Crac crac = Crac.read("crac.json", getResourceAsStream("/rao_inputs/crac.json"), network);
        final TemporalDataImpl<RaoInput> raoInputs = new TemporalDataImpl<>();
        raoInputs.put(OffsetDateTime.parse("2026-04-16T10:30Z"), RaoInput.build(network, crac).build());
        final TimeCoupledConstraints timeCoupledConstraints = new TimeCoupledConstraints();
        final TimeCoupledRaoInput timeCoupledRaoInput = new TimeCoupledRaoInput(raoInputs, timeCoupledConstraints);
        final TimeCoupledRaoResult timeCoupledRaoResult = Mockito.mock(TimeCoupledRaoResult.class);
        Mockito.when(minioAdapter.generatePreSignedUrl(filePath)).thenReturn("raoResultsUrl");

        final String resultsDestination = fileExporter.saveTimeCoupledRaoResult(timeCoupledRaoResult, timeCoupledRaoInput, timeCoupledRaoRequestWithResultDestination);

        Mockito.verify(timeCoupledRaoResult).write(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(resultsDestination).isEqualTo("raoResultsUrl");
    }

    @Test
    void saveTimeCoupledRaoResultWithResultDestinationTest() throws FileExporterException, IOException {
        final TimeCoupledRaoRequest timeCoupledRaoRequest = getTimeCoupledRaoRequestWithResultDestination(".xiidm");
        checkTimeCoupledRaoResultSaving("destination-key/raoResults.zip", timeCoupledRaoRequest);
    }

    @Test
    void saveTimeCoupledRaoResultWithoutResultDestinationTest() throws FileExporterException, IOException {
        final TimeCoupledRaoRequest timeCoupledRaoRequest = getSimpleTimeCoupledRaoRequest(".xiidm");
        checkTimeCoupledRaoResultSaving("base/path/id/raoResults.zip", timeCoupledRaoRequest);
    }

    private InputStream getResourceAsStream(final String resourcePath) {
        return Objects.requireNonNull(getClass().getResourceAsStream(resourcePath));
    }
}
