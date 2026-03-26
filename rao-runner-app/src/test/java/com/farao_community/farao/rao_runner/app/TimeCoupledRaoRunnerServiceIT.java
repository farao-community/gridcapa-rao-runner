/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.app.exceptions.FileImporterException;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TimeCoupledRaoRunnerServiceIT {

    @Autowired
    private TimeCoupledRaoRunnerService raoRunnerService;

    @Test
    void getRaoInputTest() throws FileImporterException {
        final TimeCoupledRaoRequest simpleRaoRequest = TimeCoupledTestHelper.getValidTimeCoupledRaoRequest("file:");

        final TimeCoupledRaoInputWithNetworkPaths raoInput = raoRunnerService.getRaoInput(simpleRaoRequest);

        Assertions.assertThat(raoInput).isNotNull();
        Assertions.assertThat(raoInput.getTimeCoupledConstraints().getGeneratorConstraints()).hasSize(2);
        Assertions.assertThat(raoInput.getTimeCoupledConstraints().getGeneratorConstraints())
            .anyMatch(
                constraint -> "RO_RA_00001_BBE1AA1_GENERATOR".equals(constraint.getGeneratorId())
                    && constraint.getUpwardPowerGradient().equals(Optional.of(150.0))
                    && constraint.getDownwardPowerGradient().equals(Optional.of(-250.0))
            )
            .anyMatch(
                constraint -> "RO_RA_00002_DDE1AA1_GENERATOR".equals(constraint.getGeneratorId())
                    && constraint.getUpwardPowerGradient().equals(Optional.of(150.0))
                    && constraint.getDownwardPowerGradient().equals(Optional.of(-250.0))
            );
        final List<OffsetDateTime> timestamps = List.of(
            OffsetDateTime.parse("2019-01-08T00:30+01:00"),
            OffsetDateTime.parse("2019-01-08T01:30+01:00"),
            OffsetDateTime.parse("2019-01-08T02:30+01:00"),
            OffsetDateTime.parse("2019-01-08T03:30+01:00")
        );
        Assertions.assertThat(raoInput.getTimestampsToRun()).containsExactlyInAnyOrderElementsOf(timestamps);
        Assertions.assertThat(raoInput.getRaoInputs().getDataPerTimestamp()).hasSize(4);
        for (final OffsetDateTime timestamp : timestamps) {
            final RaoInputWithNetworkPaths raoInputForTimestamp = raoInput.getRaoInputs().getData(timestamp).orElseThrow();
            Assertions.assertThat(raoInputForTimestamp.getCrac().getTimestamp()).isEqualTo(Optional.of(timestamp));
            final int hour = timestamp.getHour();
            Assertions.assertThat(raoInputForTimestamp.getInitialNetworkPath()).endsWith("timecoupled_rao_inputs/simple_case/initialNetwork_0" + hour + "30.xiidm");
            Assertions.assertThat(raoInputForTimestamp.getPostIcsImportNetworkPath()).endsWith("timecoupled_rao_inputs/simple_case/initialNetwork_0" + hour + "30.xiidm");
        }
    }
}
