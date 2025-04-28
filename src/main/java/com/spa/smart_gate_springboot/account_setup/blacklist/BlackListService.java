package com.spa.smart_gate_springboot.account_setup.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlackListService {

    private final BlackListrepository blackListRepository;

    public void addToBlacklist(String msisdn) {
        if (checkIfMsisdnBlacklisted(msisdn)) {
            log.error("Contact is already blacklisted. Msisdn: {}", msisdn);
            return;
        }
        log.error("Contact blacklisted. Msisdn: {}", msisdn);
        BlackList blackList = BlackList.builder().bcMsisdn(msisdn).build();
        blackListRepository.save(blackList);
    }

    public void removeFromBlacklist(String msisdn) {
        if (!checkIfMsisdnBlacklisted(msisdn)) {
            throw new IllegalArgumentException("Contact is not blacklisted.");
        }

        blackListRepository.deleteByBcMsisdn(msisdn);
    }


    public boolean checkIfMsisdnBlacklisted(String msisdn) {
        return blackListRepository.existsByBcMsisdn(msisdn);
    }


    public long countBlacklistedContacts() {
        return blackListRepository.count();
    }



}
