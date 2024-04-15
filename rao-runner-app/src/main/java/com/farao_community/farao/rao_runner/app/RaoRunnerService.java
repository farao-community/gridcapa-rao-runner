/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.glsk.virtual.hubs.GlskVirtualHubs;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
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
    private final Logger eventsLogger;

    public RaoRunnerService(Rao.Runner raoRunnerProvider, FileExporter fileExporter, FileImporter fileImporter, Logger eventsLogger) {
        this.raoRunnerProvider = raoRunnerProvider;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.eventsLogger = eventsLogger;
    }

    @Threadable
    public RaoResponse runRao(RaoRequest raoRequest) {
        Network network = fileImporter.importNetwork(raoRequest.getNetworkFileUrl());
        Crac crac = fileImporter.importCrac(raoRequest.getCracFileUrl(), network);
        RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest.getRaoParametersFileUrl());
        logParameters(raoParameters);
        try {
            Instant computationStartInstant = Instant.now();
            RaoResult raoResult = raoRunnerProvider.run(getRaoInput(raoRequest, network, crac), raoParameters);
            network = fileImporter.importNetwork(raoRequest.getNetworkFileUrl());
            eventsLogger.info("Applying remedial actions for preventive state");
            applyRemedialActionsForState(network, raoResult, crac.getPreventiveState());
            return saveResultsAndCreateRaoResponse(raoRequest, crac, raoResult, network, computationStartInstant, raoParameters);
        } catch (OpenRaoException e) {
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
            throw new RaoRunnerException("Exception occurred while reading RAO parameters for logging", e);
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
        Optional<String> optVirtualHubsUrl = raoRequest.getVirtualhubsFileUrl();
        if (optInstant.isPresent() && optGlskUrl.isPresent() && optRefProgUrl.isPresent() && optVirtualHubsUrl.isPresent()) {
            ReferenceProgram referenceProgram = fileImporter.importRefProg(optInstant.get(), optRefProgUrl.get());
            ZonalData<SensitivityVariableSet> glskProvider = fileImporter.importGlsk(optInstant.get(), optGlskUrl.get(), network);
            VirtualHubsConfiguration virtualHubsConfiguration = fileImporter.importVirtualHubs(optVirtualHubsUrl.get());
            ZonalData<SensitivityVariableSet> glskOfVirtualHubs = GlskVirtualHubs.getVirtualHubGlsks(virtualHubsConfiguration, network, referenceProgram);
            glskProvider.addAll(glskOfVirtualHubs);
            raoInputBuilder.withGlskProvider(glskProvider);
            raoInputBuilder.withRefProg(referenceProgram);
        }
    }

    private RaoResponse saveResultsAndCreateRaoResponse(RaoRequest raoRequest, Crac crac, RaoResult raoResult, Network network, Instant computationStartInstant, RaoParameters raoParameters) {
        String raoResultFileUrl = fileExporter.saveRaoResult(raoResult, crac, raoRequest, raoParameters.getObjectiveFunctionParameters().getType().getUnit());
        String networkWithPraFileUrl = fileExporter.saveNetwork(network, raoRequest);
        String raoInstant = raoRequest.getInstant().orElse(null);
        Instant computationEndInstant = Instant.now();
        return new RaoResponse.RaoResponseBuilder()
                .withId(raoRequest.getId())
                .withInstant(raoInstant)
                .withNetworkWithPraFileUrl(networkWithPraFileUrl)
                .withCracFileUrl(raoRequest.getCracFileUrl())
                .withRaoResultFileUrl(raoResultFileUrl)
                .withComputationStartInstant(computationStartInstant)
                .withComputationEndInstant(computationEndInstant)
                .withInterrupted(false)
                .build();
    }

    private static void applyRemedialActionsForState(Network network, RaoResult raoResult, State state) {
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction ->
            rangeAction.apply(network, raoResult.getOptimizedSetPointsOnState(state).get(rangeAction)));
    }
}
