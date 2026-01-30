/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.InterTemporalRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.InterTemporalRaoSuccessResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.TimedInput;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.intertemporalconstraints.IntertemporalConstraints;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRao;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class InterTemporalRaoRunnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterTemporalRaoRunnerService.class);

    private final InterTemporalRao.Runner raoRunnerProvider;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final Logger eventsLogger;

    public InterTemporalRaoRunnerService(InterTemporalRao.Runner raoRunnerProvider, FileExporter fileExporter, FileImporter fileImporter, Logger eventsLogger) {
        this.raoRunnerProvider = raoRunnerProvider;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.eventsLogger = eventsLogger;
    }

    @Threadable
    public AbstractRaoResponse runRao(final InterTemporalRaoRequest raoRequest) {
        try {
            final Instant computationStartInstant = Instant.now();
            final RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest.getRaoParametersFileUrl());
            logParameters(raoParameters);
            final InterTemporalRaoInputWithNetworkPaths raoInput = getRaoInput(raoRequest);

            final InterTemporalRaoResult raoResult = raoRunnerProvider.run(raoInput, raoParameters);

            if (raoResult.getComputationStatus() == ComputationStatus.FAILURE) {
                return buildRaoFailureResponse(raoRequest.getId(), "RAO computation failed");
            }
            return saveResultsAndCreateRaoResponse(raoRequest, raoInput, raoResult, computationStartInstant);
        } catch (OpenRaoException ore) {
            return buildRaoFailureResponse(raoRequest.getId(), "FARAO exception occurred when running rao: " + ore.getMessage());
        } catch (IOException | FileImporterException e) {
            return buildRaoFailureResponse(raoRequest.getId(), "Exception occurred in rao-runner: " + e.getMessage());
        }
    }

    private void logParameters(final RaoParameters raoParameters) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonRaoParameters.write(raoParameters, baos);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Running InterTemporal RAO with following parameters:{}{}", System.lineSeparator(), baos);
            }
        } catch (IOException e) {
            LOGGER.error("Exception occurred while reading RAO parameters for logging", e);
        }
    }

    private InterTemporalRaoInputWithNetworkPaths getRaoInput(final InterTemporalRaoRequest raoRequest) throws IOException, FileImporterException {
        final List<TimedInput> inputs = fileImporter.importTimedInputs(raoRequest.getTimedInputsFileUrl());

        final Map<OffsetDateTime, RaoInputWithNetworkPaths> timedInputMap = buildInputs(inputs);
        final IntertemporalConstraints intertemporalConstraints = fileImporter.importIcsFile(raoRequest.getIcsFileUrl());
        return new InterTemporalRaoInputWithNetworkPaths(new TemporalDataImpl<>(timedInputMap), intertemporalConstraints);
    }

    private static Map<OffsetDateTime, RaoInputWithNetworkPaths> buildInputs(List<TimedInput> inputs) {
        final Map<OffsetDateTime, RaoInputWithNetworkPaths> timedInputMap = new HashMap<>();
        inputs.stream().sorted(Comparator.comparing(TimedInput::timestamp))
            .forEach(timedInput -> {
                final Network network = Network.read(timedInput.networkFile());
                Crac crac = null;
                try {
                    CracCreationContext ccc = Crac.readWithContext(Path.of(timedInput.cracFile()).getFileName().toString(),
                        new FileInputStream(timedInput.cracFile()),
                        network
                    );
                    System.out.println(ccc.getCreationReport());
                    crac = ccc.getCrac();
                } catch (IOException e) {
                    System.err.println("Could not read crac: " + e.getMessage());
                    System.exit(1);
                }
                // TODO fix this. should use timedInput.ts instead of crac.ts, but it is in UTC
                timedInputMap.put(crac.getTimestamp().orElseThrow(),
                    RaoInputWithNetworkPaths.build(timedInput.networkFile(), timedInput.networkFile(), crac).build());
            });
        return timedInputMap;
    }

    private InterTemporalRaoSuccessResponse saveResultsAndCreateRaoResponse(final InterTemporalRaoRequest raoRequest,
                                                               final InterTemporalRaoInputWithNetworkPaths raoInput,
                                                               final InterTemporalRaoResult raoResult,
                                                               final Instant computationStartInstant) throws IOException {

        final String raoResultFileUrl = fileExporter.saveInterTemporalRaoResult(raoResult, raoInput, raoRequest);
        final Map<OffsetDateTime, Network> networksMithPrasMap = applyRemedialActions(raoResult, raoInput);
        final String networksWithPraFileUrl = fileExporter.saveNetwork(networksMithPrasMap, raoInput, raoRequest);

        final String raoInstant = raoRequest.getInstant().orElse(null);
        final Instant computationEndInstant = Instant.now();
        return new InterTemporalRaoSuccessResponse.Builder()
                .withId(raoRequest.getId())
                .withInstant(raoInstant)
                .withNetworksWithPraFileUrl(networksWithPraFileUrl)
                .withRaoResultsFileUrl(raoResultFileUrl)
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

    private static Map<OffsetDateTime, Network> applyRemedialActions(final InterTemporalRaoResult result, final InterTemporalRaoInputWithNetworkPaths raoInput) {
        final Map<OffsetDateTime, Network> networksMithPrasMap = new HashMap<>();

        for (final OffsetDateTime offsetDateTime : result.getTimestamps()) {
            final State preventiveState = raoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState();
            final Set<NetworkAction> preventiveNetworkActions = result.getIndividualRaoResult(offsetDateTime).getActivatedNetworkActionsDuringState(preventiveState);
            final Set<RangeAction<?>> preventiveRangeActions = result.getIndividualRaoResult(offsetDateTime).getActivatedRangeActionsDuringState(preventiveState);
            final Network network = Network.read(raoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getInitialNetworkPath());

            // Apply PRAs on network
            preventiveNetworkActions.forEach(networkAction -> networkAction.apply(network));
            preventiveRangeActions.forEach(rangeAction -> {
                final double optimizedSetpoint = result.getIndividualRaoResult(offsetDateTime).getOptimizedSetPointOnState(preventiveState, rangeAction);
                if (rangeAction instanceof final InjectionRangeAction injectionRangeAction) {
                    applyRedispatchingAction(injectionRangeAction, optimizedSetpoint, network);
                } else {
                    rangeAction.apply(network, optimizedSetpoint);
                }
            });

            networksMithPrasMap.put(offsetDateTime, network);
        }
        return networksMithPrasMap;
    }

    private static void applyRedispatchingAction(final InjectionRangeAction injectionRangeAction,
                                                 final double optimizedSetpoint,
                                                 final Network initialNetwork) {
        final double initialSetpoint = injectionRangeAction.getInitialSetpoint();
        for (final NetworkElement networkElement : injectionRangeAction.getNetworkElements()) {
            final Generator generator = initialNetwork.getGenerator(networkElement.getId());
            generator.setTargetP(generator.getTargetP()
                + (optimizedSetpoint - initialSetpoint) * injectionRangeAction.getInjectionDistributionKeys().get(networkElement));
        }
    }
}
