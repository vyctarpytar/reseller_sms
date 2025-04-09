package com.spa.smart_gate_springboot.account_setup.blacklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackListrepository extends JpaRepository<BlackList, Long> {

    boolean existsByBcMsisdn(String bcMsisdns);

    void deleteByBcMsisdn(String bcMsisdn);   // To delete a number by MSISDN
}
