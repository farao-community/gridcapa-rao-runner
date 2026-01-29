/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.InterTemporalRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.farao_community.farao.rao_runner.app.RaoResultWriterPropertiesMapper.generateJsonProperties;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileExporter {
    private static final String NETWORK = "networkWithPRA.xiidm";
    private static final String RAO_RESULT = "raoResult.json";
    private static final String IIDM_EXPORT_FORMAT = "XIIDM";
    private static final String IIDM_EXTENSION = "xiidm";

    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    String saveNetwork(final Network network, final RaoRequest raoRequest) {
        final MemDataSource dataSource = new MemDataSource();
        network.write(IIDM_EXPORT_FORMAT, null, dataSource);
        final String networkWithPRADestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + NETWORK;
        minioAdapter.uploadArtifact(networkWithPRADestinationPath, new ByteArrayInputStream(dataSource.getData(null, IIDM_EXTENSION)));
        return minioAdapter.generatePreSignedUrl(networkWithPRADestinationPath);
    }

    String saveNetwork(final Map<OffsetDateTime, Network> networksMithPrasMap,
                       final InterTemporalRaoInputWithNetworkPaths raoInput,
                       final InterTemporalRaoRequest raoRequest) throws IOException {
        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStreamRaoResult)) {
            for (final Map.Entry<OffsetDateTime, Network> entry : networksMithPrasMap.entrySet()) {
                final OffsetDateTime offsetDateTime = entry.getKey();
                final Network network = entry.getValue();
                // Write network
                final MemDataSource dataSource = new MemDataSource();
                network.write(IIDM_EXPORT_FORMAT, new Properties(), dataSource);

                // Add network to zip
                final String filePath = raoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath();
                String name = FilenameUtils.getName(filePath).split(".xiidm")[0].concat("_afterPRA.xiidm");
                ZipEntry zipEntry = new ZipEntry(name);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(dataSource.getData(null, IIDM_EXTENSION));
            }
        }
        final String networkWithPRADestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + NETWORK;
        minioAdapter.uploadArtifact(networkWithPRADestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(networkWithPRADestinationPath);
    }

    String saveRaoResult(final RaoResult raoResult, final Crac crac, final RaoRequest raoRequest, final Unit unit) {
        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, generateJsonProperties(unit), outputStreamRaoResult);
        final String raoResultDestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + RAO_RESULT;
        minioAdapter.uploadArtifact(raoResultDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(raoResultDestinationPath);
    }

    String saveInterTemporalRaoResult(final InterTemporalRaoResult raoResult,
                                      final InterTemporalRaoInputWithNetworkPaths raoInput,
                                      final InterTemporalRaoRequest raoRequest) throws IOException {
        final Properties properties = new Properties();
        properties.put("rao-result.export.json.flows-in-megawatts", "true");
        properties.put("inter-temporal-rao-result.export.filename-template", "'RAO_RESULT_'yyyy-MM-dd'T'HH:mm:ss'.json'");
        properties.put("inter-temporal-rao-result.export.summary-filename", "summary.json");

        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStreamRaoResult)) {
            raoResult.write(zipOutputStream, raoInput.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), properties);
        }

        final String resultsDestination = makeTargetDirectoryPath(raoRequest) + File.separator + RAO_RESULT;
        minioAdapter.uploadArtifact(resultsDestination, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(resultsDestination);
    }

    private String makeTargetDirectoryPath(final RaoRequest raoRequest) {
        return makeTargetDirectoryPath(raoRequest.getResultsDestination(), raoRequest.getId());
    }

    private String makeTargetDirectoryPath(final InterTemporalRaoRequest raoRequest) {
        return makeTargetDirectoryPath(raoRequest.getResultsDestination(), raoRequest.getId());
    }

    private String makeTargetDirectoryPath(final Optional<String> resultsDestination, final String raoRequestId) {
        return resultsDestination
                .orElse(minioAdapter.getProperties().getBasePath() + "/" + raoRequestId);
    }
}
