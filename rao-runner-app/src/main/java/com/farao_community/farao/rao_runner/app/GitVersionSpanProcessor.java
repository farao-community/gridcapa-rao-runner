package com.farao_community.farao.rao_runner.app;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class GitVersionSpanProcessor implements io.opentelemetry.sdk.trace.SpanProcessor {

    private final String gitVersion;

    public GitVersionSpanProcessor(String gitVersion) {
        this.gitVersion = gitVersion;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAttribute("git.version", gitVersion);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan readableSpan) {

    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
