package com.spa.smart_gate_springboot.config.logging.trace;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Copyright (c) 2016-2020, Jumia.
 */
@Getter
@Builder
public class RequestTrace {

    private static final String REQUEST_ID_FORMAT = "%s-%s";

    private final String requestId;
    private final String traceId;
    private final String parentTraceId;

    public String getId() {

        final String completeTraceId = getCompleteTraceId();

        if (StringUtils.isNotBlank(requestId) && StringUtils.isNotBlank(completeTraceId)) {

            return String.format(REQUEST_ID_FORMAT, requestId, completeTraceId);
        }

        if (StringUtils.isNotBlank(completeTraceId)) {

            return completeTraceId;
        }

        if (StringUtils.isNotBlank(requestId)) {

            return requestId;
        }

        return "";
    }

    private String getCompleteTraceId() {

        if (StringUtils.isNotBlank(parentTraceId) && StringUtils.isNotBlank(traceId)) {

            return String.format(REQUEST_ID_FORMAT, parentTraceId, traceId);
        }

        return traceId;
    }

}
