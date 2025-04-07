package com.spa.smart_gate_springboot.config.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Copyright (c) 2016 - 2020, Jumia.
 */
@Getter
@AllArgsConstructor
public enum HttpLoggingFlowContext {

    HTTP_IN("HTTP_IN"),
    HTTP_OUT("HTTP_OUT");

    private final String name;

}
