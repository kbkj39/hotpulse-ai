package com.hotpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "hotspots")
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "truth_score")
    private Double truthScore;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "importance_score")
    private Double importanceScore;

    @Column(name = "hot_score")
    private Double hotScore;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array stored as text

    @Column(name = "analysis_evidence", columnDefinition = "TEXT")
    private String analysisEvidence;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
