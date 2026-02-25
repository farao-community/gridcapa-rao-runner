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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TimeCoupledRaoRunnerServiceTest {

    @Autowired
    TimeCoupledRaoRunnerService raoRunnerService;
    @MockitoBean
    FileExporter fileExporter;

    @Test
    void checkSuccessfulSimpleRaoRun() {
        final TimeCoupledRaoRequest simpleRaoRequest = new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("id")
            .withTimedInputsFileUrl("file:" + getClass().getResource("/timecoupled_rao_inputs/simple_case/timed-inputs.json").getPath())
            .withIcsFileUrl("file:" + getClass().getResource("/timecoupled_rao_inputs/simple_case/timecoupled-constraints.json").getPath())
            .withRaoParametersFileUrl("file:" + getClass().getResource("/timecoupled_rao_inputs/simple_case/RaoParameters.json").getPath())
            .withResultsDestination("timecoupled_rao_results")
            .build();

        final AbstractRaoResponse abstractRaoResponse = raoRunnerService.runRao(simpleRaoRequest);

        Assertions.assertThat(abstractRaoResponse).isNotNull();

//
//        Assertions.assertThat(raoInputCaptor.getValue())
//            .isNotNull()
//            .hasFieldOrPropertyWithValue("crac", crac)
//            .hasFieldOrPropertyWithValue("network", network)
//            .hasFieldOrPropertyWithValue("glsk", null)
//            .hasFieldOrPropertyWithValue("referenceProgram", null);
        Assertions.assertThat(abstractRaoResponse)
            .isNotNull()
            .hasFieldOrPropertyWithValue("raoFailed", false);
        final TimeCoupledRaoSuccessResponse raoResponse = (TimeCoupledRaoSuccessResponse) abstractRaoResponse;
        Assertions.assertThat(raoResponse)
            .hasFieldOrPropertyWithValue("id", "id")
            .hasFieldOrPropertyWithValue("instant", Optional.empty())
//            .hasFieldOrPropertyWithValue("networkWithPraFileUrl", "simple-networkWithPRA-url")
//            .hasFieldOrPropertyWithValue("cracFileUrl", "http://host:9000/crac.json")
//            .hasFieldOrPropertyWithValue("raoResultFileUrl", "simple-RaoResultJson-url")
            .hasFieldOrPropertyWithValue("interrupted", false);
        checkComputationStartAndEndInstants(raoResponse);
    }

    private static void checkComputationStartAndEndInstants(final TimeCoupledRaoSuccessResponse raoResponse) {
        final Instant now = Instant.now();
        Assertions.assertThat(raoResponse.getComputationStartInstant())
            .isBetween(now.minus(1, ChronoUnit.MINUTES), now);
        Assertions.assertThat(raoResponse.getComputationEndInstant())
            .isBetween(now.minus(1, ChronoUnit.MINUTES), now);
        Assertions.assertThat(raoResponse.getComputationStartInstant())
            .isBeforeOrEqualTo(raoResponse.getComputationEndInstant());
    }
}
