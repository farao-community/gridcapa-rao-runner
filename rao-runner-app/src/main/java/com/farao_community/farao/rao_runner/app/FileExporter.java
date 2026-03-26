/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.exceptions.FileExporterException;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.farao_community.farao.rao_runner.app.RaoResultWriterPropertiesMapper.generateJsonProperties;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileExporter {
    private static final Properties RAO_RESULT_EXPORT_PROPERTIES = new Properties();
    private static final Pattern NETWORK_FILENAME_PATTERN = Pattern.compile("(?<filenameNoExt>.*)\\.(?<extension>[bjx]iidm)");

    static {
        RAO_RESULT_EXPORT_PROPERTIES.put("rao-result.export.json.flows-in-megawatts", "true");
        RAO_RESULT_EXPORT_PROPERTIES.put("time-coupled-rao-result.export.filename-template", "'RAO_RESULT_'yyyy-MM-dd'T'HH:mm:ss'.json'");
        RAO_RESULT_EXPORT_PROPERTIES.put("time-coupled-rao-result.export.summary-filename", "summary.json");
    }

    private static final String NETWORK_XIIDM = "networkWithPRA.xiidm";
    private static final String NETWORKS_ZIP = "networksWithPRA.zip";
    private static final String RAO_RESULT_JSON = "raoResult.json";
    private static final String RAO_RESULTS_ZIP = "raoResults.zip";
    private static final String XIIDM_EXPORT_FORMAT = "XIIDM";
    private static final String XIIDM_EXTENSION = "xiidm";

    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    String saveNetwork(final Network network, final RaoRequest raoRequest) {
        final MemDataSource dataSource = new MemDataSource();
        network.write(XIIDM_EXPORT_FORMAT, null, dataSource);
        final String networkWithPRADestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + NETWORK_XIIDM;
        minioAdapter.uploadArtifact(networkWithPRADestinationPath, new ByteArrayInputStream(dataSource.getData(null, XIIDM_EXTENSION)));
        return minioAdapter.generatePreSignedUrl(networkWithPRADestinationPath);
    }

    String saveNetworks(final Map<OffsetDateTime, Network> networksWithPrasMap,
                        final TimeCoupledRaoInputWithNetworkPaths raoInput,
                        final TimeCoupledRaoRequest raoRequest) throws FileExporterException {
        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStreamRaoResult)) {
            for (final Map.Entry<OffsetDateTime, Network> entry : networksWithPrasMap.entrySet()) {
                final OffsetDateTime offsetDateTime = entry.getKey();
                final Network network = entry.getValue();
                // Write network
                addNetworkToZip(network, zipOutputStream, raoInput, offsetDateTime);
            }
        } catch (final IOException ioe) {
            throw new FileExporterException("Error occurred while trying to export time coupled networks", ioe);
        }
        final String networksWithPraDestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + NETWORKS_ZIP;
        minioAdapter.uploadArtifact(networksWithPraDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(networksWithPraDestinationPath);
    }

    private static void addNetworkToZip(final Network network,
                                        final ZipOutputStream zipOutputStream,
                                        final TimeCoupledRaoInputWithNetworkPaths raoInput,
                                        final OffsetDateTime offsetDateTime) throws IOException, FileExporterException {
        final String networkFilePath = raoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath();
        final String networkFilename = FilenameUtils.getName(networkFilePath);
        final Matcher matcher = NETWORK_FILENAME_PATTERN.matcher(networkFilename);
        final String outputExtension;
        final String filenameNoExt;
        if (matcher.find()) {
            outputExtension = matcher.group("extension");
            filenameNoExt = matcher.group("filenameNoExt");
        } else {
            final int lastDotPosition = networkFilename.lastIndexOf(".");
            final int index = lastDotPosition == -1 ? networkFilename.length() : lastDotPosition;
            outputExtension = networkFilename.substring(Math.min(index + 1, networkFilename.length()));
            throw new FileExporterException("Unsupported network format \"%s\" with filename %s".formatted(outputExtension, networkFilename));
        }

        final String outputFilename = filenameNoExt.concat("_afterPRA.").concat(outputExtension);
        final String outputFormat = outputExtension.toUpperCase();
        final ZipEntry zipEntry = new ZipEntry(outputFilename);
        zipOutputStream.putNextEntry(zipEntry);

        final MemDataSource networkDataSource = new MemDataSource();
        network.write(outputFormat, new Properties(), networkDataSource);

        zipOutputStream.write(networkDataSource.getData(null, outputExtension));
    }

    String saveRaoResult(final RaoResult raoResult, final Crac crac, final RaoRequest raoRequest, final Unit unit) {
        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, generateJsonProperties(unit), outputStreamRaoResult);
        final String raoResultDestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + RAO_RESULT_JSON;
        minioAdapter.uploadArtifact(raoResultDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(raoResultDestinationPath);
    }

    String saveTimeCoupledRaoResult(final TimeCoupledRaoResult raoResult,
                                    final TimeCoupledRaoInputWithNetworkPaths raoInput,
                                    final TimeCoupledRaoRequest raoRequest) throws FileExporterException {
        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStreamRaoResult)) {
            raoResult.write(zipOutputStream, raoInput.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), RAO_RESULT_EXPORT_PROPERTIES);
        } catch (final IOException ioe) {
            throw new FileExporterException("Error occurred while trying to export time coupled rao result", ioe);
        }

        final String resultDestination = makeTargetDirectoryPath(raoRequest) + File.separator + RAO_RESULTS_ZIP;
        minioAdapter.uploadArtifact(resultDestination, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(resultDestination);
    }

    private String makeTargetDirectoryPath(final RaoRequest raoRequest) {
        return makeTargetDirectoryPath(raoRequest.getResultsDestination(), raoRequest.getId());
    }

    private String makeTargetDirectoryPath(final TimeCoupledRaoRequest raoRequest) {
        return makeTargetDirectoryPath(raoRequest.getResultsDestination(), raoRequest.getId());
    }

    private String makeTargetDirectoryPath(final Optional<String> resultsDestination, final String raoRequestId) {
        return resultsDestination
                .orElse(minioAdapter.getProperties().getBasePath() + "/" + raoRequestId);
    }
}
