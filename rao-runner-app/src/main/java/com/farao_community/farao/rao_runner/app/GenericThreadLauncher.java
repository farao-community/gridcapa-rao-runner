/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.ThreadLauncherResult;
import org.slf4j.MDC;

public class GenericThreadLauncher<T, U> extends Thread {

    private final T threadable;
    private final Method run;
    private final Object[] args;
    private final Map<String, String> contextMap;
    private ThreadLauncherResult<U> result;

    public GenericThreadLauncher(T threadable, String id, Map<String, String> contextMap, Object... args) {
        super(id);
        this.run = getMethodAnnotatedWith(threadable.getClass());
        this.threadable = threadable;
        this.args = args;
        this.contextMap = new HashMap<>(contextMap);
    }

    @Override
    public void run() {
        try {
            contextMap.forEach(MDC::put);
            U threadResult = (U) this.run.invoke(threadable, args);
            this.result = ThreadLauncherResult.success(threadResult);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (result == null) {
                this.result = ThreadLauncherResult.error(e);
            }
        }
    }

    @Override
    public void interrupt() {
        this.result = ThreadLauncherResult.interrupt();
        super.interrupt();
    }

    public ThreadLauncherResult<U> getResult() {
        try {
            join();
        } catch (InterruptedException e) {
            interrupt();
        }
        return result;
    }

    private static Method getMethodAnnotatedWith(final Class<?> type) {
        List<Method> methods = getMethodsAnnotatedWith(type);
        if (methods.isEmpty()) {
            throw new RaoRunnerException("the class " + type.getCanonicalName() + " does not have his running method annotated with @Threadable");
        } else if (methods.size() > 1) {
            throw new RaoRunnerException("the class " + type.getCanonicalName() + " must have only one method annotated with @Threadable");
        } else {
            return methods.get(0);
        }
    }

    private static List<Method> getMethodsAnnotatedWith(final Class<?> type) {
        final List<Method> methods = new ArrayList<>();
        Class<?> klass = type;
        while (klass != Object.class) { // need to traverse a type hierarchy in order to process methods from super types
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Threadable.class)) {
                    methods.add(method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

}
