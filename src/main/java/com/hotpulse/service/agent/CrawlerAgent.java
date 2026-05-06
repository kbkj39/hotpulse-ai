package com.hotpulse.service.agent;

import com.hotpulse.common.AgentConstants;
import com.hotpulse.entity.Document;
import com.hotpulse.skill.FetchAndIngestSkill;
import com.hotpulse.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerAgent {

    private final FetchAndIngestSkill fetchAndIngestSkill;
    private final AgentExecutionTracker tracker;

    public Document crawl(Long executionId, String url, Long sourceId, Instant publishedAt) {
        tracker.recordStep(executionId, AgentConstants.CRAWLER_AGENT, AgentConstants.STATUS_RUNNING,
                "正在抓取: " + url, null);
        try {
            SkillResult<Document> result = fetchAndIngestSkill.execute(
                    new FetchAndIngestSkill.Input(url, sourceId, publishedAt));
            if (result.isOk() && result.data() != null) {
                tracker.recordStep(executionId, AgentConstants.CRAWLER_AGENT, AgentConstants.STATUS_DONE,
                        "抓取完成: " + url, null);
                return result.data();
            } else {
                String errMsg = result.error() != null ? result.error() : "重复内容或空内容";
                tracker.recordStep(executionId, AgentConstants.CRAWLER_AGENT, AgentConstants.STATUS_FAILED,
                        "抓取跳过: " + url + " - " + errMsg, null);
                return null;
            }
        } catch (Exception e) {
            log.error("CrawlerAgent failed for url: {}", url, e);
            tracker.recordStep(executionId, AgentConstants.CRAWLER_AGENT, AgentConstants.STATUS_FAILED,
                    "抓取异常: " + url + " - " + e.getMessage(), null);
            return null;
        }
    }
}
