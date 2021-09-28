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
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class RaoRunnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoRunnerService.class);

    private final Rao.Runner raoRunnerProvider;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;

    public RaoRunnerService(Rao.Runner raoRunnerProvider, FileExporter fileExporter, FileImporter fileImporter) {
        this.raoRunnerProvider = raoRunnerProvider;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
    }

    public RaoResponse runRao(RaoRequest raoRequest) {
        Network network = fileImporter.importNetwork(raoRequest);
        Crac crac = fileImporter.importCrac(raoRequest);
        Optional<ZonalData<LinearGlsk>> glskProvider = fileImporter.importGlsk(raoRequest, network);
        Optional<ReferenceProgram> referenceProgram = fileImporter.importRefProg(raoRequest);
        RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest);
        logParameters(raoParameters);
        return runRao(raoRequest, network, crac, glskProvider, referenceProgram, raoParameters);
    }

    private void logParameters(RaoParameters raoParameters) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Running RAO with following parameters:{}{}", System.lineSeparator(), baos);
        }
    }

    public RaoResponse runRao(RaoRequest raoRequest,
                              Network network,
                              Crac crac,
                              Optional<ZonalData<LinearGlsk>> glsks,
                              Optional<ReferenceProgram> referenceProgram,
                              RaoParameters raoParameters) {
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
            RaoResult raoResult = raoRunnerProvider.run(raoInputBuilder.build(), raoParameters);
            return uploadRaoResultsToFileStorageServer(raoRequest, crac, raoResult, network);
        } catch (Exception e) {
            throw new RaoRunnerException("Error occurred when running rao: " + e.getMessage(), e);
        }
    }

    private RaoResponse uploadRaoResultsToFileStorageServer(RaoRequest raoRequest, Crac crac, RaoResult raoResult, Network network) {
        String resultsDestination = fileExporter.generateResultsDestination(raoRequest);
        String raoResultFileUrl = fileExporter.exportAndSaveJsonRaoResult(raoResult, crac, resultsDestination);
        applyRemedialActionsForState(network, raoResult, crac.getPreventiveState());
        String networkWithPraFileUrl = fileExporter.exportAndSaveNetworkWithPra(raoResult, network, resultsDestination);
        String instant = raoRequest.getInstant().orElse(null);
        return new RaoResponse(raoRequest.getId(), instant, networkWithPraFileUrl, raoRequest.getCracFileUrl(), raoResultFileUrl);
    }

    private static void applyRemedialActionsForState(Network network, RaoResult raoResult, State state) {
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(network));
        raoResult.getOptimizedSetPointsOnState(state).forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));
    }
}
