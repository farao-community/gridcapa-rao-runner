/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.api.resource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class TimeCoupledRaoSuccessResponseTest {

    @Test
    void checkRaoRequestNormalUsage() {
        TimeCoupledRaoSuccessResponse raoResponse = new TimeCoupledRaoSuccessResponse.Builder()
                .withId("id")
                .withNetworksWithPraFileUrl("networksWithPraFileUrl")
                .withRaoResultsFileUrl("raoResultsFileUrl")
                .withComputationStartInstant(Instant.ofEpochSecond(3600))
                .withComputationEndInstant(Instant.ofEpochSecond(7200))
                .withInterrupted(true)
                .build();
        Assertions.assertThat(raoResponse).isNotNull();
        Assertions.assertThat(raoResponse.getId()).isEqualTo("id");
        Assertions.assertThat(raoResponse.getNetworksWithPraFileUrl()).isEqualTo("networksWithPraFileUrl");
        Assertions.assertThat(raoResponse.getRaoResultsFileUrl()).isEqualTo("raoResultsFileUrl");
        Assertions.assertThat(raoResponse.getComputationStartInstant()).isEqualTo(Instant.ofEpochSecond(3600));
        Assertions.assertThat(raoResponse.getComputationEndInstant()).isEqualTo(Instant.ofEpochSecond(7200));
        Assertions.assertThat(raoResponse.isInterrupted()).isTrue();
    }
}
