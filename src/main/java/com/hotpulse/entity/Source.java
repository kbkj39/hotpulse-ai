package com.hotpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // RSS / HTML / API

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "reputation_score")
    private Double reputationScore = 1.0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "robots_allowed")
    private Boolean robotsAllowed = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
