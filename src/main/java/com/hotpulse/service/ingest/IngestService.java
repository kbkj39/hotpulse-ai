package com.hotpulse.service.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.entity.Document;
import com.hotpulse.entity.RawPage;
import com.hotpulse.entity.Source;
import com.hotpulse.repository.DocumentRepository;
import com.hotpulse.repository.RawPageRepository;
import com.hotpulse.repository.SourceRepository;
import com.hotpulse.service.crawler.JsoupFetcher;
import com.hotpulse.skill.SummarizeSkill;
import com.hotpulse.skill.TagDocumentSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final JsoupFetcher jsoupFetcher;
    private final DocumentCleaner documentCleaner;
    private final DuplicateDetector duplicateDetector;
    private final SummarizeSkill summarizeSkill;
    private final TagDocumentSkill tagDocumentSkill;
    private final SourceRepository sourceRepository;
    private final ObjectMapper objectMapper;
    private final RawPageRepository rawPageRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public Document ingest(String url, Long sourceId) {
        // 1. URL 去重：已存在则直接返回已入库的 Document，供 Agent 继续使用
        if (duplicateDetector.isDuplicateUrl(url)) {
            log.info("URL already ingested, reusing existing document: {}", url);
            return documentRepository.findBySourceUrl(url).orElse(null);
        }

        // 2. 抓取原始内容
        String rawHtml = jsoupFetcher.fetchContent(url);
        if (rawHtml == null || rawHtml.isBlank()) {
            log.warn("Empty content fetched from: {}", url);
            return null;
        }

        // 3. 内容去重（SHA-256 指纹）
        String fingerprint = duplicateDetector.computeFingerprint(rawHtml);
        if (duplicateDetector.isDuplicateContent(rawHtml)) {
            log.info("Duplicate content detected for url: {}", url);
            return rawPageRepository.findByFingerprint(fingerprint)
                    .flatMap(rp -> documentRepository.findByRawPageId(rp.getId()))
                    .orElse(null);
        }

        // 4. 持久化原始页面
        RawPage rawPage = new RawPage();
        rawPage.setSourceId(sourceId != null ? sourceId : 0L);
        rawPage.setUrl(url);
        rawPage.setCanonicalUrl(url);
        rawPage.setFingerprint(fingerprint);
        rawPage.setStatus("FETCHED");
        rawPage.setRawContent(rawHtml);
        rawPage.setFetchedAt(Instant.now());
        rawPage = rawPageRepository.save(rawPage);

        // 5. 清洗正文
        String title = documentCleaner.extractTitle(rawHtml);
        String content = documentCleaner.clean(rawHtml);

        // 6. 生成摘要
        String summary = "";
        var summaryResult = summarizeSkill.execute(content);
        if (summaryResult.isOk()) {
            summary = summaryResult.data();
        }

        // 7. 生成标签
        List<String> tags = List.of();
        var tagResult = tagDocumentSkill.execute(title + "\n" + content);
        if (tagResult.isOk()) {
            tags = tagResult.data();
        }

        // 8. 持久化清洗后文档
        String sourceName = null;
        if (sourceId != null && sourceId > 0) {
            sourceName = sourceRepository.findById(sourceId).map(Source::getName).orElse(null);
        }

        String tagsJson = "[]";
        if (!tags.isEmpty()) {
            try {
                tagsJson = objectMapper.writeValueAsString(tags);
            } catch (Exception e) {
                log.warn("Failed to serialize tags for url: {}", url);
            }
        }

        Document document = new Document();
        document.setRawPageId(rawPage.getId());
        document.setTitle(title.isBlank() ? url : title);
        document.setContent(content);
        document.setPublishedAt(Instant.now());
        document.setSummary(summary);
        document.setSourceUrl(url);
        document.setSourceName(sourceName);
        document.setTagsJson(tagsJson);
        Document saved = documentRepository.save(document);
        log.info("Ingested document id={} from url={}", saved.getId(), url);
        return saved;
    }
}
