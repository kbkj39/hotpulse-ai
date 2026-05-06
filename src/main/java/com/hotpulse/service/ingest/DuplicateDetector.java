package com.hotpulse.service.ingest;

import com.hotpulse.repository.RawPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class DuplicateDetector {

    private final RawPageRepository rawPageRepository;

    public boolean isDuplicateUrl(String canonicalUrl) {
        return rawPageRepository.existsByCanonicalUrl(canonicalUrl);
    }

    public boolean isDuplicateContent(String content) {
        String fingerprint = sha256(content);
        return rawPageRepository.existsByFingerprint(fingerprint);
    }

    public String computeFingerprint(String content) {
        return sha256(content);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }
}
