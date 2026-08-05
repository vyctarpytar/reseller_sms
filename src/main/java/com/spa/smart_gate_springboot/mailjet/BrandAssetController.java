package com.spa.smart_gate_springboot.mailjet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

// Public + absolute-URL addressable because Gmail's image proxy fetches email images
// out-of-band over a plain unauthenticated GET.
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/public/brand")
public class BrandAssetController {

    private static final String LOGO_PATH = "branding/logo.png";

    @GetMapping(value = "/logo.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> logo() {

        try (InputStream in = new ClassPathResource(LOGO_PATH).getInputStream()) {

            byte[] bytes = in.readAllBytes();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .body(bytes);

        } catch (Exception e) {
            log.error("Unable to stream brand logo {} : {}", LOGO_PATH, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
