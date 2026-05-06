package com.hotpulse.repository;

import com.hotpulse.entity.Hotspot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {

    Page<Hotspot> findAll(Pageable pageable);

    @Query("""
            SELECT h FROM Hotspot h, Document d
            WHERE d.id = h.documentId
              AND (CAST(:tag AS string) IS NULL OR h.tags LIKE CONCAT('%', CAST(:tag AS string), '%'))
              AND (CAST(:keyword AS string) IS NULL
                   OR LOWER(COALESCE(d.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.summary, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.content, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.sourceName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(h.tags, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            ORDER BY h.hotScore DESC
            """)
    Page<Hotspot> findByTagAndKeywordOrderByHotScore(@Param("tag") String tag, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT h FROM Hotspot h, Document d
            WHERE d.id = h.documentId
              AND (CAST(:tag AS string) IS NULL OR h.tags LIKE CONCAT('%', CAST(:tag AS string), '%'))
              AND (CAST(:keyword AS string) IS NULL
                   OR LOWER(COALESCE(d.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.summary, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.content, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.sourceName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(h.tags, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            ORDER BY h.importanceScore DESC
            """)
    Page<Hotspot> findByTagAndKeywordOrderByImportanceScore(@Param("tag") String tag, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT h FROM Hotspot h, Document d
            WHERE d.id = h.documentId
              AND (CAST(:tag AS string) IS NULL OR h.tags LIKE CONCAT('%', CAST(:tag AS string), '%'))
              AND (CAST(:keyword AS string) IS NULL
                   OR LOWER(COALESCE(d.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.summary, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.content, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.sourceName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(h.tags, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            ORDER BY h.relevanceScore DESC
            """)
    Page<Hotspot> findByTagAndKeywordOrderByRelevanceScore(@Param("tag") String tag, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT h FROM Hotspot h, Document d
            WHERE d.id = h.documentId
              AND (CAST(:tag AS string) IS NULL OR h.tags LIKE CONCAT('%', CAST(:tag AS string), '%'))
              AND (CAST(:keyword AS string) IS NULL
                   OR LOWER(COALESCE(d.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.summary, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.content, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(d.sourceName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(COALESCE(h.tags, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            ORDER BY h.createdAt DESC
            """)
    Page<Hotspot> findByTagAndKeywordOrderByCreatedAtDesc(@Param("tag") String tag, @Param("keyword") String keyword, Pageable pageable);
}
