/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Set;

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

    String saveNetwork(Network network, RaoRequest raoRequest) {
        MemDataSource dataSource = new MemDataSource();
        network.write(IIDM_EXPORT_FORMAT, null, dataSource);
        String networkWithPRADestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + NETWORK;
        minioAdapter.uploadArtifact(networkWithPRADestinationPath, new ByteArrayInputStream(dataSource.getData(null, IIDM_EXTENSION)));
        return minioAdapter.generatePreSignedUrl(networkWithPRADestinationPath);
    }

    String saveRaoResult(RaoResult raoResult, Crac crac, RaoRequest raoRequest, Unit unit) {
        ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, Set.of(unit), outputStreamRaoResult);
        String raoResultDestinationPath = makeTargetDirectoryPath(raoRequest) + File.separator + RAO_RESULT;
        minioAdapter.uploadArtifact(raoResultDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(raoResultDestinationPath);
    }

    private String makeTargetDirectoryPath(RaoRequest raoRequest) {
        return raoRequest.getResultsDestination()
                .orElse(minioAdapter.getProperties().getBasePath() + "/" + raoRequest.getId());
    }
}
