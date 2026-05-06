package com.hotpulse.repository;

import com.hotpulse.entity.RawPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RawPageRepository extends JpaRepository<RawPage, Long> {
    boolean existsByFingerprint(String fingerprint);
    boolean existsByCanonicalUrl(String canonicalUrl);
    List<RawPage> findByStatus(String status);
    Optional<RawPage> findByFingerprint(String fingerprint);
}
