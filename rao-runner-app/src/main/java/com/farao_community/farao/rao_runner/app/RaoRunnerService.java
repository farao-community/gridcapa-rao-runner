/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.FaraoException;
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

import java.io.*;
import java.time.Instant;
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
        Network network = fileImporter.importNetwork(raoRequest.getNetworkFileUrl());
        Crac crac = fileImporter.importCrac(raoRequest.getCracFileUrl());
        RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest.getRaoParametersFileUrl());
        logParameters(raoParameters);
        try {
            Instant computationStartInstant = Instant.now();
            RaoResult raoResult = raoRunnerProvider.run(getRaoInput(raoRequest, network, crac), raoParameters);
            applyRemedialActionsForState(network, raoResult, crac.getPreventiveState());
            return saveResultsAndCreateRaoResponse(raoRequest, crac, raoResult, network, computationStartInstant);
        } catch (FaraoException e) {
            throw new RaoRunnerException("FARAO exception occurred when running rao: " + e.getMessage(), e);
        }
    }

    private void logParameters(RaoParameters raoParameters) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonRaoParameters.write(raoParameters, baos);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Running RAO with following parameters:{}{}", System.lineSeparator(), baos);
            }
        } catch (IOException e) {
            throw new RaoRunnerException(String.format("Exception occur while reading RAO parameters for logging: %s", e.getMessage()));
        }
    }

    private RaoInput getRaoInput(RaoRequest raoRequest, Network network, Crac crac) {
        RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        addCoreD2CCInputsIfPresent(raoRequest, raoInputBuilder, network);
        return raoInputBuilder.build();
    }

    private void addCoreD2CCInputsIfPresent(RaoRequest raoRequest, RaoInput.RaoInputBuilder raoInputBuilder, Network network) {
        Optional<String> optInstant = raoRequest.getInstant();
        Optional<String> optGlskUrl = raoRequest.getRealGlskFileUrl();
        Optional<String> optRefProgUrl = raoRequest.getRefprogFileUrl();
        if (optInstant.isPresent() && optGlskUrl.isPresent() && optRefProgUrl.isPresent()) {
            ReferenceProgram referenceProgram = fileImporter.importRefProg(optInstant.get(), optRefProgUrl.get());
            ZonalData<LinearGlsk> glskProvider = fileImporter.importGlsk(optInstant.get(), optGlskUrl.get(), network);
            ZonalData<LinearGlsk> glskOfVirtualHubs = GlskVirtualHubs.getVirtualHubGlsks(network, referenceProgram);
            glskProvider.addAll(glskOfVirtualHubs);
            raoInputBuilder.withGlskProvider(glskProvider);
            raoInputBuilder.withRefProg(referenceProgram);
        }
    }

    private RaoResponse saveResultsAndCreateRaoResponse(RaoRequest raoRequest, Crac crac, RaoResult raoResult, Network network, Instant computationStartInstant) {
        String raoResultFileUrl = fileExporter.saveRaoResult(raoResult, crac, raoRequest);
        String networkWithPraFileUrl = fileExporter.saveNetwork(network, raoRequest);
        String raoInstant = raoRequest.getInstant().orElse(null);
        Instant computationEndInstant = Instant.now();
        return new RaoResponse(raoRequest.getId(),
                raoInstant,
                networkWithPraFileUrl,
                raoRequest.getCracFileUrl(),
                raoResultFileUrl,
                computationStartInstant,
                computationEndInstant);
    }

    private static void applyRemedialActionsForState(Network network, RaoResult raoResult, State state) {
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(network));
        raoResult.getOptimizedSetPointsOnState(state).forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));
    }
}
