package com.hotpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "raw_pages")
public class RawPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(nullable = false, length = 64)
    private String fingerprint; // SHA-256

    @Column(nullable = false)
    private String status; // PENDING / FETCHED / FAILED

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
