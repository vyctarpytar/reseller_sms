package com.spa.smart_gate_springboot.config.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Copyright (c) 2016-2019, Jumia.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoggingContext {

    private String format;
    private Object[] arguments;
    private FlowLoggingContext flowLoggingContext;

}
