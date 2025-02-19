/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.glsk.virtual.hubs.GlskVirtualHubs;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
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
    public AbstractRaoResponse runRao(final RaoRequest raoRequest) {
        try {
            final Instant computationStartInstant = Instant.now();
            final RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest.getRaoParametersFileUrl());
            logParameters(raoParameters);

            Network network = fileImporter.importNetwork(raoRequest.getNetworkFileUrl());
            final Crac crac = fileImporter.importCrac(raoRequest.getCracFileUrl(), network);
            final RaoInput raoInput = getRaoInput(raoRequest, network, crac);
            final RaoResult raoResult = raoRunnerProvider.run(raoInput, raoParameters);

            if (raoResult.getComputationStatus() == ComputationStatus.FAILURE) {
                return buildRaoFailureResponse(raoRequest.getId(), "RAO computation failed");
            }
            network = fileImporter.importNetwork(raoRequest.getNetworkFileUrl());
            eventsLogger.info("Applying remedial actions for preventive state");
            applyRemedialActionsForState(network, raoResult, crac.getPreventiveState());
            return saveResultsAndCreateRaoResponse(raoRequest, crac, raoResult, network, computationStartInstant, raoParameters);
        } catch (OpenRaoException ore) {
            return buildRaoFailureResponse(raoRequest.getId(), "FARAO exception occurred when running rao: " + ore.getMessage());
        } catch (FileImporterException fie) {
            return buildRaoFailureResponse(raoRequest.getId(), "Exception occurred in rao-runner: " + fie.getMessage());
        }
    }

    private void logParameters(final RaoParameters raoParameters) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonRaoParameters.write(raoParameters, baos);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Running RAO with following parameters:{}{}", System.lineSeparator(), baos);
            }
        } catch (IOException e) {
            LOGGER.error("Exception occurred while reading RAO parameters for logging", e);
        }
    }

    private RaoInput getRaoInput(final RaoRequest raoRequest, final Network network, final Crac crac) throws FileImporterException {
        final RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        addCoreD2CCInputsIfPresent(raoRequest, raoInputBuilder, network);
        return raoInputBuilder.build();
    }

    private void addCoreD2CCInputsIfPresent(final RaoRequest raoRequest, final RaoInput.RaoInputBuilder raoInputBuilder, final Network network) throws FileImporterException {
        final Optional<String> optInstant = raoRequest.getInstant();
        final Optional<String> optGlskUrl = raoRequest.getRealGlskFileUrl();
        final Optional<String> optRefProgUrl = raoRequest.getRefprogFileUrl();
        final Optional<String> optVirtualHubsUrl = raoRequest.getVirtualhubsFileUrl();
        if (optInstant.isPresent() && optGlskUrl.isPresent() && optRefProgUrl.isPresent() && optVirtualHubsUrl.isPresent()) {
            final ReferenceProgram referenceProgram = fileImporter.importRefProg(optInstant.get(), optRefProgUrl.get());
            final ZonalData<SensitivityVariableSet> glskProvider = fileImporter.importGlsk(optInstant.get(), optGlskUrl.get(), network);
            final VirtualHubsConfiguration virtualHubsConfiguration = fileImporter.importVirtualHubs(optVirtualHubsUrl.get());
            final ZonalData<SensitivityVariableSet> glskOfVirtualHubs = GlskVirtualHubs.getVirtualHubGlsks(virtualHubsConfiguration, network, referenceProgram);
            glskProvider.addAll(glskOfVirtualHubs);
            raoInputBuilder.withGlskProvider(glskProvider);
            raoInputBuilder.withRefProg(referenceProgram);
        }
    }

    private static void applyRemedialActionsForState(final Network network, final RaoResult raoResult, final State state) {
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction ->
                rangeAction.apply(network, raoResult.getOptimizedSetPointsOnState(state).get(rangeAction)));
    }

    private RaoSuccessResponse saveResultsAndCreateRaoResponse(final RaoRequest raoRequest, final Crac crac, final RaoResult raoResult, final Network network, final Instant computationStartInstant, final RaoParameters raoParameters) {
        final String raoResultFileUrl = fileExporter.saveRaoResult(raoResult, crac, raoRequest, raoParameters.getObjectiveFunctionParameters().getUnit());
        final String networkWithPraFileUrl = fileExporter.saveNetwork(network, raoRequest);
        final String raoInstant = raoRequest.getInstant().orElse(null);
        final Instant computationEndInstant = Instant.now();
        return new RaoSuccessResponse.Builder()
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

    private RaoFailureResponse buildRaoFailureResponse(final String id, final String message) {
        return new RaoFailureResponse.Builder()
                .withId(id)
                .withErrorMessage(message)
                .build();
    }
}
