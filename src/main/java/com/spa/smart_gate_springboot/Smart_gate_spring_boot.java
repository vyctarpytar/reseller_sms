package com.spa.smart_gate_springboot;

import com.spa.smart_gate_springboot.utils.AppTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@Slf4j
public class Smart_gate_spring_boot {
    public static void main(String[] args) {
        // FIRST statement: Hibernate's @CreationTimestamp captures the VM clock during bootstrap, so
        // the zone has to be pinned before Spring starts. See AppTime.
        AppTime.install();
        SpringApplication.run(Smart_gate_spring_boot.class, args);
        log.info("----YYYYY-------Application started");
    }

}
