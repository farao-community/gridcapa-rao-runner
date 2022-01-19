/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.starter;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import static com.farao_community.farao.rao_runner.starter.MCDContextWrapper.wrapWithMdcContext;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class MDCAwareForkJoinPool extends ForkJoinPool {

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return super.submit(wrapWithMdcContext(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return super.submit(wrapWithMdcContext(task), result);
    }

    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        return super.submit(wrapWithMdcContext(task));
    }

    @Override
    public void execute(Runnable task) {
        super.execute(wrapWithMdcContext(task));
    }
}
