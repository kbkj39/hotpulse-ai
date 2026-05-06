package com.hotpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_page_id", nullable = false)
    private Long rawPageId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "author")
    private String author;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
