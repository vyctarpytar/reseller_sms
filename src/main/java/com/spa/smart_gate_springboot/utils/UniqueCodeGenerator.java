package com.spa.smart_gate_springboot.utils;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.Base64;
public class UniqueCodeGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 24;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> GENERATED_CODES = new HashSet<>();


    public String getUniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (GENERATED_CODES.contains(code));

        GENERATED_CODES.add(code);
        return code;
    }

    private String generateCode() {
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            codeBuilder.append(CHARACTERS.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public  String generateSecureApiKey() {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
