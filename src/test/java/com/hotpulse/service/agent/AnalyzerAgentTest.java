package com.hotpulse.service.agent;

import com.hotpulse.entity.Document;
import com.hotpulse.service.iwencai.IwencaiSkillService;
import com.hotpulse.skill.VerifyTruthfulnessSkill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AnalyzerAgentTest {

    private final AnalyzerAgent analyzerAgent = new AnalyzerAgent(
            mock(IwencaiSkillService.class),
            mock(VerifyTruthfulnessSkill.class),
            mock(AgentExecutionTracker.class));

    @Test
    void relevanceIsStrongWhenPrimaryKeywordMatches() {
        AnalyzerAgent.RelevanceResult result = analyzerAgent.computeKeywordRelevance(
                doc("opai 生态更新", "opai 开发者工具发布"),
                "opai openai chatgpt");

        assertThat(result.score()).isGreaterThanOrEqualTo(0.9);
        assertThat(result.reason()).contains("opai");
    }

    @Test
    void relevancePassesWhenExpansionKeywordMatchesButPrimaryDoesNot() {
        AnalyzerAgent.RelevanceResult result = analyzerAgent.computeKeywordRelevance(
                doc("OpenAI 发布新模型", "ChatGPT 相关进展"),
                "opai openai chatgpt");

        assertThat(result.score()).isGreaterThanOrEqualTo(0.45);
        assertThat(result.reason()).contains("openai");
    }

    @Test
    void relevanceIsLowWhenNoKeywordMatches() {
        AnalyzerAgent.RelevanceResult result = analyzerAgent.computeKeywordRelevance(
                doc("世界杯赞助商变化", "中国企业加码体育营销"),
                "opai openai chatgpt");

        assertThat(result.score()).isLessThan(0.45);
        assertThat(result.reason()).contains("未命中");
    }

    @Test
    void originalMonitorKeywordOnlyPreventsBroadAiMatches() {
        AnalyzerAgent.RelevanceResult result = analyzerAgent.computeKeywordRelevance(
                doc("AI 正在制造认知灾难", "人工智能影响青少年教育"),
                "opai");

        assertThat(result.score()).isLessThan(0.45);
    }

    private static Document doc(String title, String summary) {
        Document doc = new Document();
        doc.setTitle(title);
        doc.setSummary(summary);
        doc.setContent(summary);
        return doc;
    }
}

