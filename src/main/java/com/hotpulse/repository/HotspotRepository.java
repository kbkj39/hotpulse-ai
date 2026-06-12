package com.hotpulse.repository;

import com.hotpulse.entity.Hotspot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {

    Page<Hotspot> findAll(Pageable pageable);

    @Query("""
            SELECT h FROM Hotspot h
            WHERE h.createdAt >= :start AND h.createdAt < :end
            ORDER BY h.importanceScore DESC
            """)
    Page<Hotspot> findByCreatedAtBetweenOrderByImportanceScoreDesc(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

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

    @Query(value = """
            WITH filtered AS (
                SELECT h.id,
                       h.hot_score,
                       h.importance_score,
                       h.created_at,
                       DATE_TRUNC(:interval, h.created_at) AS time_bucket
                FROM hotspots h
                JOIN documents d ON d.id = h.document_id
                LEFT JOIN agent_executions ae ON ae.id = h.execution_id
                WHERE h.created_at >= :start_time
                  AND (CAST(:monitor_keyword AS text) IS NULL OR ae.query = CONCAT('监控抓取: ', CAST(:monitor_keyword AS text)))
                  AND (CAST(:tag AS text) IS NULL OR h.tags LIKE CONCAT('%', CAST(:tag AS text), '%'))
                  AND (CAST(:keyword AS text) IS NULL
                       OR LOWER(COALESCE(d.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
                       OR LOWER(COALESCE(d.summary, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
                       OR LOWER(COALESCE(d.content, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
                       OR LOWER(COALESCE(d.source_name, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
                       OR LOWER(COALESCE(h.tags, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
                       OR LOWER(COALESCE(ae.query, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
            ),
            ranked AS (
                SELECT *,
                       ROW_NUMBER() OVER (
                           PARTITION BY time_bucket
                           ORDER BY hot_score DESC, importance_score DESC, created_at DESC
                       ) AS rank_in_bucket
                FROM filtered
            ),
            counts AS (
                SELECT time_bucket, COUNT(*) AS count
                FROM filtered
                GROUP BY time_bucket
            ),
            top_scores AS (
                SELECT time_bucket,
                       AVG(hot_score) AS avg_hot_score,
                       AVG(importance_score) AS avg_importance_score
                FROM ranked
                WHERE rank_in_bucket <= 5
                GROUP BY time_bucket
            )
            SELECT c.time_bucket,
                   c.count,
                   t.avg_hot_score,
                   t.avg_importance_score
            FROM counts c
            JOIN top_scores t ON t.time_bucket = c.time_bucket
            ORDER BY c.time_bucket
            """, nativeQuery = true)
    List<Object[]> getTrendStats(
            @Param("interval") String interval,
            @Param("start_time") Instant startTime,
            @Param("monitor_keyword") String monitorKeyword,
            @Param("tag") String tag,
            @Param("keyword") String keyword);
}
