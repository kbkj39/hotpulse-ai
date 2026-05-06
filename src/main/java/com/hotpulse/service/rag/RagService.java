package com.hotpulse.service.rag;

import com.hotpulse.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorSearchService vectorSearchService;
    private final ChatClient chatClient;

    public SearchResponse query(String query, int topK) {
        List<SearchResponse.Evidence> evidences = vectorSearchService.search(query, topK);

        SearchResponse response = new SearchResponse();
        response.setEvidences(evidences);

        // 知识库暂无向量化内容时，跳过 LLM 调用，避免浪费 API 配额
        if (evidences.isEmpty()) {
            response.setAnswer(null); // 由 AgentOrchestrator 用热点数据生成回答
            return response;
        }

        String context = evidences.stream()
                .map(e -> "- " + e.getSnippet() + "\n  来源: " + e.getUrl())
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是一个财经分析助手。请根据以下参考资料回答用户的问题。
                回答需基于参考资料，不要编造信息。若参考资料不足以回答，请如实说明。
                
                参考资料：
                %s
                
                用户问题：%s
                """.formatted(context, query);

        String answer;
        try {
            answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("RAG chat failed for query: {}", query, e);
            answer = null; // 由 AgentOrchestrator 用热点数据生成回答
        }

        response.setAnswer(answer);
        return response;
    }
}
