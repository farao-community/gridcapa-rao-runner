/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;

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

    String saveRaoResult(final RaoResult raoResult, final Crac crac, final RaoRequest raoRequest, final Unit unit) {
        final ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        final String propertiesPrefix = "rao-result.export.json.flows-in-";
        final Properties properties = new Properties();
        if (Unit.AMPERE == unit) {
            properties.setProperty(propertiesPrefix + "amperes", "true");
        } else {
            properties.setProperty(propertiesPrefix + "megawatts", "true");
        }
        raoResult.write("JSON", crac, properties, outputStreamRaoResult);
        final String raoResultDestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + RAO_RESULT;
        minioAdapter.uploadArtifact(raoResultDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(raoResultDestinationPath);
    }

    private String makeTargetDirectoryPath(final RaoRequest raoRequest) {
        return raoRequest.getResultsDestination()
                .orElse(minioAdapter.getProperties().getBasePath() + "/" + raoRequest.getId());
    }
}
