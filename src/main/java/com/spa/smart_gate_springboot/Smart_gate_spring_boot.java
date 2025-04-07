package com.spa.smart_gate_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class Smart_gate_spring_boot {
    public static void main(String[] args) {
        SpringApplication.run(Smart_gate_spring_boot.class, args);
    }

}
