/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.TimeCoupledRaoRequest;
import com.farao_community.farao.rao_runner.api.resource.TimedInput;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class TimeCoupledTestHelper {
    private TimeCoupledTestHelper() {

    }

    public static TimeCoupledRaoRequest getValidTimeCoupledRaoRequest(final String filePathPrefix) {
        final List<TimedInput> timedInputs = new ArrayList<>();
        timedInputs.add(new TimedInput(
            OffsetDateTime.parse("2019-01-08T00:30:00+01:00"),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/initialNetwork_0030.xiidm", filePathPrefix),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/crac_0030.json", filePathPrefix)));
        timedInputs.add(new TimedInput(
            OffsetDateTime.parse("2019-01-08T01:30:00+01:00"),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/initialNetwork_0130.xiidm", filePathPrefix),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/crac_0130.json", filePathPrefix)));
        timedInputs.add(new TimedInput(
            OffsetDateTime.parse("2019-01-08T02:30:00+01:00"),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/initialNetwork_0230.xiidm", filePathPrefix),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/crac_0230.json", filePathPrefix)));
        timedInputs.add(new TimedInput(
            OffsetDateTime.parse("2019-01-08T03:30:00+01:00"),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/initialNetwork_0330.xiidm", filePathPrefix),
            getResourceUrl("/timecoupled_rao_inputs/simple_case/crac_0330.json", filePathPrefix)));

        return new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("raoRequestId")
            .withTimedInputs(timedInputs)
            .withIcsFileUrl(getResourceUrl("/timecoupled_rao_inputs/simple_case/timeCoupledConstraints.json", filePathPrefix))
            .withRaoParametersFileUrl(getResourceUrl("/timecoupled_rao_inputs/simple_case/raoParameters.json", filePathPrefix))
            .withResultsDestination("timecoupled_rao_results")
            .build();
    }

    private static String getResourceUrl(final String resourceRelativePath, final String filePathPrefix) {
        final URL resourceUrl = Objects.requireNonNull(TimeCoupledTestHelper.class.getResource(resourceRelativePath));
        return filePathPrefix + resourceUrl.getPath();
    }
}
