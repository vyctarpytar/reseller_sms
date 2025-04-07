package com.spa.smart_gate_springboot.config.logging;


import com.spa.smart_gate_springboot.config.logging.trace.RequestTrace;
import com.spa.smart_gate_springboot.config.logging.trace.RequestTraceContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Optional;

public class LoggingFactory {

    private static final String COUNTRY_KEY_NAME = "COUNTRY";
    private static final String DEFAULT_COUNTRY = "all";
    private static final String FLOW_KEY_NAME = "FLOW";
    private static final String FLOW_STATUS_KEY_NAME = "FLOW_STATUS";
    private static final String REQUEST_ID_KEY_NAME = "REQ_ID";

    public static void debug(final Logger log, final Optional<String> country, final FlowLoggingContext flowLoggingContext,
        final String format, final Object... arguments) {

        if (!log.isDebugEnabled()) {

            return;
        }

        LoggingFactory.debug(log, LoggingContext.builder()
            .flowLoggingContext(flowLoggingContext)
            .format(format)
            .arguments(arguments)
            .build());
    }

    public static void debug(final Logger log, final LoggingContext loggingContext) {

        if (!log.isDebugEnabled()) {

            return;
        }

        addLoggingKeys(loggingContext);
        log.debug(loggingContext.getFormat(), loggingContext.getArguments());
        removeLoggingKeys();
    }

    public static void info(final Logger log, final FlowLoggingContext flowLoggingContext,
        final String format, final Object... arguments) {

        if (!log.isInfoEnabled()) {

            return;
        }

        LoggingFactory.info(log, LoggingContext.builder()
            .flowLoggingContext(flowLoggingContext)
            .format(format)
            .arguments(arguments)
            .build());
    }

    public static void info(final Logger log, final LoggingContext loggingContext) {

        if (!log.isInfoEnabled()) {

            return;
        }

        addLoggingKeys(loggingContext);
        log.info(loggingContext.getFormat(), loggingContext.getArguments());
        removeLoggingKeys();
    }

    public static void warn(final Logger log, final Optional<String> country, final FlowLoggingContext flowLoggingContext,
        final String format, final Object... arguments) {

        if (!log.isWarnEnabled()) {

            return;
        }

        LoggingFactory.warn(log, LoggingContext.builder()
            .flowLoggingContext(flowLoggingContext)
            .format(format)
            .arguments(arguments)
            .build());
    }

    public static void warn(final Logger log, final LoggingContext loggingContext) {

        if (!log.isWarnEnabled()) {

            return;
        }

        addLoggingKeys(loggingContext);
        log.warn(loggingContext.getFormat(), loggingContext.getArguments());
        removeLoggingKeys();
    }

    public static void error(final Logger log,  final FlowLoggingContext flowLoggingContext,
        final String format, final Object... arguments) {

        if (!log.isErrorEnabled()) {

            return;
        }

        LoggingFactory.error(log, LoggingContext.builder()
            .flowLoggingContext(flowLoggingContext)
            .format(format)
            .arguments(arguments)
            .build());
    }

    public static void error(final Logger log, final LoggingContext loggingContext) {

        if (!log.isErrorEnabled()) {

            return;
        }

        addLoggingKeys(loggingContext);
        log.error(loggingContext.getFormat(), loggingContext.getArguments());
        removeLoggingKeys();
    }

    private static void addLoggingKeys(final LoggingContext loggingContext) {

        MDC.put(FLOW_KEY_NAME, loggingContext.getFlowLoggingContext().getFlow());
        MDC.put(FLOW_STATUS_KEY_NAME, loggingContext.getFlowLoggingContext().getFlowStatus());
        MDC.put(REQUEST_ID_KEY_NAME, getRequestId());
    }

    private static String getRequestId() {

        final Optional<RequestTrace> requestTrace = RequestTraceContext.getCurrentTrace();

        if (requestTrace.isPresent() && StringUtils.isNotBlank(requestTrace.get().getId())) {

            return requestTrace.get().getId();
        }

        return FlowLoggingContext.NOT_AVAILABLE;
    }

    private static void removeLoggingKeys() {

        MDC.remove(COUNTRY_KEY_NAME);
        MDC.remove(FLOW_KEY_NAME);
        MDC.remove(FLOW_STATUS_KEY_NAME);
        MDC.remove(REQUEST_ID_KEY_NAME);
    }


}
