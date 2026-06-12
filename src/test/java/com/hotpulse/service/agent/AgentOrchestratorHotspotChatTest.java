package com.hotpulse.service.agent;

import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.repository.MessageRepository;
import com.hotpulse.service.hotspot.HotspotService;
import com.hotpulse.service.hotspot.KeywordExpansionService;
import com.hotpulse.repository.SourceRepository;
import com.hotpulse.service.iwencai.IwencaiSkillService;
import com.hotpulse.sse.AgentSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorHotspotChatTest {

    @Mock private PlannerAgent plannerAgent;
    @Mock private SearcherAgent searcherAgent;
    @Mock private CrawlerAgent crawlerAgent;
    @Mock private AnalyzerAgent analyzerAgent;
    @Mock private AggregatorAgent aggregatorAgent;
    @Mock private AgentExecutionTracker tracker;
    @Mock private AgentExecutionService executionService;
    @Mock private AgentSseService agentSseService;
    @Mock private SourceRepository sourceRepository;
    @Mock private IwencaiSkillService iwencaiSkillService;
    @Mock private HotspotService hotspotService;
    @Mock private KeywordExpansionService keywordExpansionService;
    @Mock private MessageRepository messageRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private ExecutorService virtualThreadExecutor;
    @Mock private ChatClient chatClient;

    @InjectMocks
    private AgentOrchestrator agentOrchestrator;

    @Test
    void executeWithHotspotIdSkipsPlannerAndUsesHotspotChat() {
        Long executionId = 42L;
        Long hotspotId = 7L;
        HotspotResponse hotspot = new HotspotResponse();
        hotspot.setId(hotspotId);
        hotspot.setTitle("测试热点");
        hotspot.setSummary("摘要");
        hotspot.setSource("测试源");
        hotspot.setUrl("https://example.com/news");
        hotspot.setFullText("正文内容");

        when(hotspotService.getHotspotDetail(hotspotId)).thenReturn(hotspot);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(anyLong()))
                .thenReturn(java.util.List.of());

        ChatClient.ChatClientRequestSpec requestSpec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("基于热点的回答");

        agentOrchestrator.execute(executionId, "这条新闻可信吗？", 1L, hotspotId);

        verify(plannerAgent, never()).plan(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(hotspotService).getHotspotDetail(hotspotId);
        verify(tracker).recordStep(eq(executionId), eq("HotspotChat"), eq("RUNNING"),
                org.mockito.ArgumentMatchers.contains("加载热点"), org.mockito.ArgumentMatchers.isNull());
        verify(executionService).markDone(executionId);
    }
}
