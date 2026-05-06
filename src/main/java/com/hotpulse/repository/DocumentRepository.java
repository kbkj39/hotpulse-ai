package com.hotpulse.repository;

import com.hotpulse.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByPublishedAtBetween(Instant from, Instant to);
    Optional<Document> findBySourceUrl(String sourceUrl);
    Optional<Document> findByRawPageId(Long rawPageId);
}
