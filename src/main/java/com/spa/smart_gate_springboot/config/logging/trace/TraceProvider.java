package com.spa.smart_gate_springboot.config.logging.trace;

/**
 * Copyright (c) 2016-2020, Jumia.
 */
public interface TraceProvider {

    /**
     * It returns the generated trace id of the request.
     *
     * @return the trace Id.
     */
    String getTraceId();

    /**
     * Set the request id as a custom new relic metric attribute.
     *
     * @param requestId to be registered at the current new relic trace transaction.
     */
    void setRequestId(String requestId);

}
