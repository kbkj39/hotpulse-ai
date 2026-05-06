package com.hotpulse.service.hotspot;

import com.hotpulse.dto.MonitorKeywordCreateResponse;
import com.hotpulse.dto.MonitorKeywordRequest;
import com.hotpulse.entity.AgentExecution;
import com.hotpulse.entity.MonitorKeyword;
import com.hotpulse.repository.MonitorKeywordRepository;
import com.hotpulse.service.agent.AgentExecutionService;
import com.hotpulse.service.agent.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorKeywordService {

    private final MonitorKeywordRepository monitorKeywordRepository;
    private final AgentExecutionService agentExecutionService;
    private final ExecutorService virtualThreadExecutor;
    @Lazy
    private final AgentOrchestrator agentOrchestrator;

    public List<MonitorKeyword> list() {
        return monitorKeywordRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    public MonitorKeywordCreateResponse create(MonitorKeywordRequest input) {
        String normalized = normalize(input.getKeyword());
        monitorKeywordRepository.findByKeywordIgnoreCase(normalized).ifPresent(existing -> {
            throw new IllegalArgumentException("监控关键词已存在: " + normalized);
        });

        MonitorKeyword keyword = new MonitorKeyword();
        keyword.setKeyword(normalized);
        keyword.setEnabled(input.getEnabled() == null || input.getEnabled());
        keyword.setCrawlIntervalHours(
                input.getCrawlIntervalHours() != null && input.getCrawlIntervalHours() > 0
                        ? input.getCrawlIntervalHours() : null);

        MonitorKeyword saved = monitorKeywordRepository.save(keyword);

        Long executionId = null;
        if (Boolean.TRUE.equals(input.getTriggerNow()) && saved.getEnabled()) {
            executionId = triggerCrawlForKeyword(saved);
        }
        return new MonitorKeywordCreateResponse(saved, executionId);
    }

    public MonitorKeyword update(Long id, MonitorKeywordRequest input) {
        MonitorKeyword existing = monitorKeywordRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("监控关键词不存在: " + id));

        if (input.getKeyword() != null && !input.getKeyword().isBlank()) {
            String normalized = normalize(input.getKeyword());
            monitorKeywordRepository.findByKeywordIgnoreCase(normalized)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("监控关键词已存在: " + normalized);
                    });
            existing.setKeyword(normalized);
        }

        if (input.getEnabled() != null) {
            existing.setEnabled(input.getEnabled());
        }

        if (input.getCrawlIntervalHours() != null) {
            existing.setCrawlIntervalHours(input.getCrawlIntervalHours() > 0 ? input.getCrawlIntervalHours() : null);
        }

        return monitorKeywordRepository.save(existing);
    }

    public void delete(Long id) {
        if (!monitorKeywordRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("监控关键词不存在: " + id);
        }
        monitorKeywordRepository.deleteById(id);
    }

    /**
     * 通过 id 立即触发一次抓取。
     *
     * @return 创建的 AgentExecution ID
     */
    public Long triggerById(Long id) {
        MonitorKeyword kw = monitorKeywordRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("监控关键词不存在: " + id));
        return triggerCrawlForKeyword(kw);
    }

    /**
     * 立即触发所有已启用关键词的抓取。
     *
     * @return 每个关键词对应的 keywordId -> executionId 列表
     */
    public List<java.util.Map<String, Object>> triggerAll() {
        return monitorKeywordRepository
                .findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .filter(kw -> Boolean.TRUE.equals(kw.getEnabled()))
                .map(kw -> {
                    Long execId = triggerCrawlForKeyword(kw);
                    return java.util.Map.<String, Object>of("keywordId", kw.getId(), "executionId", execId);
                })
                .toList();
    }

    /**
     * 立即触发该关键词的一次搜索抓取。
     * 先更新 lastCrawledAt 以防定时器重复触发，再异步执行管线。
     *
     * @return 创建的 AgentExecution ID
     */
    public Long triggerCrawlForKeyword(MonitorKeyword kw) {
        kw.setLastCrawledAt(Instant.now());
        monitorKeywordRepository.save(kw);

        AgentExecution execution = agentExecutionService.create("监控抓取: " + kw.getKeyword(), null);
        Long executionId = execution.getId();
        virtualThreadExecutor.execute(() ->
                agentOrchestrator.executeScheduled(executionId, List.of(kw.getKeyword()))
        );
        log.info("MonitorKeywordService: 已触发关键词「{}」的抓取，executionId={}", kw.getKeyword(), executionId);
        return executionId;
    }

    private String normalize(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("监控关键词不能为空");
        }
        return normalized;
    }
}
