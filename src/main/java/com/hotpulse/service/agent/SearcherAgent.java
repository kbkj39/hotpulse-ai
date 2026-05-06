package com.hotpulse.service.agent;

import com.hotpulse.common.AgentConstants;
import com.hotpulse.entity.Source;
import com.hotpulse.service.crawler.CandidateItem;
import com.hotpulse.service.crawler.PageFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearcherAgent {

    private final PageFetcher pageFetcher;
    private final AgentExecutionTracker tracker;

    public List<CandidateItem> search(Long executionId, Source source, List<String> keywords) {
        tracker.recordStep(executionId, AgentConstants.SEARCHER_AGENT, AgentConstants.STATUS_RUNNING,
                "正在搜索信息源: " + source.getName(), null);
        try {
            List<CandidateItem> candidates = pageFetcher.fetchCandidates(source, keywords);
            tracker.recordStep(executionId, AgentConstants.SEARCHER_AGENT, AgentConstants.STATUS_DONE,
                    "从 " + source.getName() + " 获取到 " + candidates.size() + " 条候选", null);
            return candidates;
        } catch (Exception e) {
            log.error("SearcherAgent failed for source: {}", source.getName(), e);
            tracker.recordStep(executionId, AgentConstants.SEARCHER_AGENT, AgentConstants.STATUS_FAILED,
                    "搜索失败: " + source.getName() + " - " + e.getMessage(), null);
            return Collections.emptyList();
        }
    }
}
