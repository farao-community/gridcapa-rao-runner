package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;

public interface AbstractRaoRunnerService {
    default RaoFailureResponse buildRaoFailureResponse(final String id, final String message) {
        return new RaoFailureResponse.Builder()
            .withId(id)
            .withErrorMessage(message)
            .build();
    }
}
