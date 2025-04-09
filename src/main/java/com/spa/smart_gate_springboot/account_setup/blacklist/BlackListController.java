package com.spa.smart_gate_springboot.account_setup.blacklist;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/blacklist")
@RequiredArgsConstructor
@Slf4j
public class BlackListController {
    private final BlackListService blackListService;


    @GetMapping({"/{msisdn}"})
    public ResponseEntity<String> getPhoneNumber(@PathVariable String msisdn) {
        var isBlacklisted = blackListService.checkIfMsisdnBlacklisted(msisdn);

        Map<String, Object> mapp = new HashMap<>();
        mapp.put("isBlacklisted", isBlacklisted);
        mapp.put("msisdn", msisdn);
        return ResponseEntity.ok().body(mapp.toString());
    }

    @PostMapping({"/{msisdn}"})
    public ResponseEntity<String> addToBlackList(@PathVariable String msisdn) {
        var isBlacklisted = blackListService.checkIfMsisdnBlacklisted(msisdn);
        if (!isBlacklisted) {
            blackListService.addToBlacklist(msisdn);
            isBlacklisted = true;
        }

        Map<String, Object> mapp = new HashMap<>();
        mapp.put("isBlacklisted", isBlacklisted);
        mapp.put("msisdn", msisdn);
        return ResponseEntity.ok().body(mapp.toString());
    }

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeFromBlacklist(@RequestParam String msisdn) {
        blackListService.removeFromBlacklist(msisdn);
        return ResponseEntity.ok("Contact removed from blacklist.");
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countBlacklistedContacts() {
        long count = blackListService.countBlacklistedContacts();
        return ResponseEntity.ok(count);
    }

}
