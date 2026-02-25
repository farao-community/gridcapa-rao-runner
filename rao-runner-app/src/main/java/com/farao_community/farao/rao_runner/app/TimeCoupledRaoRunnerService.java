/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoSuccessResponse;
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
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.TimeCoupledRao;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
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
public class TimeCoupledRaoRunnerService implements AbstractRaoRunnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeCoupledRaoRunnerService.class);

    private final TimeCoupledRao.Runner raoRunnerProvider;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;

    public TimeCoupledRaoRunnerService(TimeCoupledRao.Runner raoRunnerProvider, FileExporter fileExporter, FileImporter fileImporter) {
        this.raoRunnerProvider = raoRunnerProvider;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
    }

    @Threadable
    public AbstractRaoResponse runRao(final TimeCoupledRaoRequest raoRequest) {
        try {
            final Instant computationStartInstant = Instant.now();
            final RaoParameters raoParameters = fileImporter.importRaoParameters(raoRequest.getRaoParametersFileUrl());
            logParameters(raoParameters);
            final TimeCoupledRaoInputWithNetworkPaths raoInput = getRaoInput(raoRequest);

            final TimeCoupledRaoResult raoResult = raoRunnerProvider.run(raoInput, raoParameters);

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
                LOGGER.info("Running time-coupled RAO with following parameters:{}{}", System.lineSeparator(), baos);
            }
        } catch (IOException e) {
            LOGGER.error("Exception occurred while reading RAO parameters for logging", e);
        }
    }

    private TimeCoupledRaoInputWithNetworkPaths getRaoInput(final TimeCoupledRaoRequest raoRequest) throws FileImporterException {
        final List<TimedInput> timedInputs = fileImporter.importTimedInputs(raoRequest.getTimedInputsFileUrl());
        final Map<OffsetDateTime, RaoInputWithNetworkPaths> timedInputMap = buildInputs(timedInputs);
        final TimeCoupledConstraints timeCoupledConstraints = fileImporter.importIcsFile(raoRequest.getIcsFileUrl());
        return new TimeCoupledRaoInputWithNetworkPaths(new TemporalDataImpl<>(timedInputMap), timeCoupledConstraints);
    }

    private Map<OffsetDateTime, RaoInputWithNetworkPaths> buildInputs(final List<TimedInput> inputs) throws FileImporterException {
        final Map<OffsetDateTime, RaoInputWithNetworkPaths> timedInputMap = new HashMap<>();
        final List<TimedInput> sortedTimedInputs = inputs.stream()
            .sorted(Comparator.comparing(TimedInput::timestamp))
            .toList();

        for (final TimedInput timedInput : sortedTimedInputs) {
            final Network network = fileImporter.importNetwork(timedInput.networkFile());
            // TODO Besoin de pré-processing sur le network ici ?
            final Crac crac = fileImporter.importCracWithContext(timedInput.cracFile(), network);

            // TODO fix this. should use timedInput.ts instead of crac.ts, but it is in UTC
            timedInputMap.put(crac.getTimestamp().orElseThrow(),
                RaoInputWithNetworkPaths.build(timedInput.networkFile(), timedInput.networkFile(), crac).build());
        }
        return timedInputMap;
    }

    private TimeCoupledRaoSuccessResponse saveResultsAndCreateRaoResponse(final TimeCoupledRaoRequest raoRequest,
                                                                          final TimeCoupledRaoInputWithNetworkPaths raoInput,
                                                                          final TimeCoupledRaoResult raoResult,
                                                                          final Instant computationStartInstant) throws IOException {

        final String raoResultFileUrl = fileExporter.saveTimeCoupledRaoResult(raoResult, raoInput, raoRequest);
        final Map<OffsetDateTime, Network> networksWithPrasMap = applyRemedialActions(raoResult, raoInput);
        final String networksWithPraFileUrl = fileExporter.saveNetwork(networksWithPrasMap, raoInput, raoRequest);

        final String raoInstant = raoRequest.getInstant().orElse(null); // TODO Vérifier si on a une valeur mais à mon avis ça n'a pas de sens pour de l'intertemporel donc on devrait pouvoir retirer l'appel à withInstant() ci-dessous
        final Instant computationEndInstant = Instant.now();
        return new TimeCoupledRaoSuccessResponse.Builder()
            .withId(raoRequest.getId())
            .withInstant(raoInstant)
            .withNetworksWithPraFileUrl(networksWithPraFileUrl)
            .withRaoResultsFileUrl(raoResultFileUrl)
            .withComputationStartInstant(computationStartInstant)
            .withComputationEndInstant(computationEndInstant)
            .withInterrupted(false)
            .build();
    }

    private static Map<OffsetDateTime, Network> applyRemedialActions(final TimeCoupledRaoResult result, final TimeCoupledRaoInputWithNetworkPaths raoInput) {
        final Map<OffsetDateTime, Network> networksWithPrasMap = new HashMap<>();

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

            networksWithPrasMap.put(offsetDateTime, network);
        }
        return networksWithPrasMap;
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
