package com.spa.smart_gate_springboot.config.logging;

/**
 * Copyright (c) 2016 - 2020, Jumia.
 */
public interface FlowLoggingContext {

    String SUCCESS = "SUCCESS";
    String FAILURE = "FAILURE";
    String NOT_AVAILABLE = "-";

    String getFlow();

    String getFlowStatus();

}
