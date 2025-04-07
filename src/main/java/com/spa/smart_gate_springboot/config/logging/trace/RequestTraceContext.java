package com.spa.smart_gate_springboot.config.logging.trace;

import java.util.Optional;

/**
 * Copyright (c) 2016-2018, Jumia.
 */
public final class RequestTraceContext {

    private static final InheritableThreadLocal<RequestTrace> CURRENT_TRACE = new InheritableThreadLocal<>();

    public static void setCurrentTrace(final RequestTrace requestTrace) {

        CURRENT_TRACE.set(requestTrace);
    }

    public static Optional<RequestTrace> getCurrentTrace() {

        return Optional.ofNullable(CURRENT_TRACE.get());
    }

    public static void clearCurrentTrace() {

        CURRENT_TRACE.set(null);
    }

}
