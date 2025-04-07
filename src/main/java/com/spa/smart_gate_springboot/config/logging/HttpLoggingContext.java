package com.spa.smart_gate_springboot.config.logging;


import com.spa.smart_gate_springboot.config.logging.trace.RequestTrace;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Copyright (c) 2016 - 2020, Jumia.
 */
public final class HttpLoggingContext implements FlowLoggingContext {

    private static final String NOT_AVAILABLE = "-";
    private HttpLoggingFlowContext flow;
    private Optional<HttpStatus> status;
    private Optional<RequestTrace> requestTrace;

    public static HttpLoggingContext inHttpContext(final HttpStatus status) {

        final HttpLoggingContext httpLoggingContext = new HttpLoggingContext();
        httpLoggingContext.flow = HttpLoggingFlowContext.HTTP_IN;
        httpLoggingContext.status = Optional.ofNullable(status);

        return httpLoggingContext;
    }

    public static HttpLoggingContext outHttpContext(final Optional<HttpStatus> status) {

        final HttpLoggingContext httpLoggingContext = new HttpLoggingContext();
        httpLoggingContext.flow = HttpLoggingFlowContext.HTTP_OUT;
        httpLoggingContext.status = status;

        return httpLoggingContext;
    }

    @Override
    public String getFlow() {

        if (!Objects.isNull(flow)) {

            return flow.name();
        }

        return NOT_AVAILABLE;
    }

    @Override
    public String getFlowStatus() {

        if (!Objects.isNull(status) && status.isPresent()) {

            return String.valueOf(status.get().value());
        }

        return NOT_AVAILABLE;
    }

}
