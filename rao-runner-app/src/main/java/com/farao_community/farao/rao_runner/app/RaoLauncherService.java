/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.glsk.virtual.hubs.GlskVirtualHubs;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultExporter;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class RaoLauncherService {
    private static final String NETWORK = "networkWithPRA.xiidm";
    private static final String RAO_RESULT = "raoResult.json";
    private static final String IIDM_EXPORT_FORMAT = "XIIDM";
    private static final String IIDM_EXTENSION = "xiidm";

    private final MinioAdapter minioAdapter;

    public RaoLauncherService(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public RaoResponse runRao(RaoRequest raoRequest,
                              Network network,
                              Crac crac,
                              Optional<ZonalData<LinearGlsk>> glsks,
                              Optional<ReferenceProgram> referenceProgram,
                              RaoParameters raoParameters,
                              String resultsDestination) {
        try {
            RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
            glsks.ifPresent(raoInputBuilder::withGlskProvider);
            referenceProgram.ifPresent(raoInputBuilder::withRefProg);

            if (glsks.isPresent() && referenceProgram.isPresent()) {
                ZonalData<LinearGlsk> glskOfVirtualHubs = GlskVirtualHubs.getVirtualHubGlsks(network, referenceProgram.get());
                glsks.get().addAll(glskOfVirtualHubs);
                raoInputBuilder.withGlskProvider(glsks.get());
            }

            // Run search tree rao
            RaoResult results = Rao.run(raoInputBuilder.build(), raoParameters);
            return uploadRaoResultsToFileStorageServer(raoRequest, crac, results, network, resultsDestination);
        } catch (Exception e) {
            throw new RaoRunnerException("Error occurred when running rao: " + e.getMessage(), e);
        }
    }

    private RaoResponse uploadRaoResultsToFileStorageServer(RaoRequest raoRequest, Crac crac, RaoResult raoResult, Network network, String resultsDestination) {
        String raoResultFileUrl = exportAndSaveJsonRaoResult(raoResult, crac, resultsDestination);
        String networkWithPraFileUrl = exportAndSaveNetworkWithPra(raoResult, network, crac, resultsDestination);
        String instant = raoRequest.getInstant();
        return new RaoResponse(raoRequest.getId(), instant, networkWithPraFileUrl, raoRequest.getCracFileUrl(), raoResultFileUrl);
    }

    private String exportAndSaveNetworkWithPra(RaoResult raoResult, Network network, Crac crac, String resultsDestination) {
        String networkWithPraFileUrl;
        if (Objects.nonNull(raoResult) && raoResult.getComputationStatus() != ComputationStatus.FAILURE) {
            MemDataSource dataSource = new MemDataSource();
            applyRemedialActionsForState(network, raoResult, crac.getPreventiveState());
            Exporters.export(IIDM_EXPORT_FORMAT, network, null, dataSource);
            String networkWithPRADestinationPath = resultsDestination + File.separator + NETWORK;
            minioAdapter.uploadFile(networkWithPRADestinationPath, new ByteArrayInputStream(dataSource.getData(null, IIDM_EXTENSION)));
            networkWithPraFileUrl = minioAdapter.generatePreSignedUrl(networkWithPRADestinationPath);
            return networkWithPraFileUrl;
        } else {
            throw new RaoRunnerException("Cannot find optimized network in rao result");
        }
    }

    private String exportAndSaveJsonRaoResult(RaoResult raoResult, Crac crac, String resultsDestination) {
        ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, outputStreamRaoResult);
        String raoResultDestinationPath = resultsDestination + File.separator + RAO_RESULT;
        minioAdapter.uploadFile(raoResultDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(raoResultDestinationPath);
    }

    private static void applyRemedialActionsForState(Network network, RaoResult raoResult, State state) {
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(network));
        raoResult.getOptimizedSetPointsOnState(state).forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));
    }
}
