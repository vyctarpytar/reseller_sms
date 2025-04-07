package com.spa.smart_gate_springboot.messaging.operatorPrefix;

import com.spa.smart_gate_springboot.errorhandling.ApplicationExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class  OperatorPrefixService {

    private final OperatorPrefixRepository operatorPrefixRepository;

    public OperatorPrefix findById(long id) {
        return this.operatorPrefixRepository.findById(id).orElseThrow(() -> new ApplicationExceptionHandler.resourceNotFoundException("OperatorPrefix not found with Id : " + id));
    }


    public OperatorPrefix findByOpPrefixAndOpOperator(long opPrefix, String opOperator) {
        return this.operatorPrefixRepository.findByOpPrefixAndOpOperator(opPrefix, opOperator);
    }
}


