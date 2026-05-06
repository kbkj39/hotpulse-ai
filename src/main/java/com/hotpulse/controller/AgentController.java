package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.AgentQueryRequest;
import com.hotpulse.dto.AgentQueryResponse;
import com.hotpulse.entity.AgentExecution;
import com.hotpulse.entity.Conversation;
import com.hotpulse.entity.Message;
import com.hotpulse.repository.ConversationRepository;
import com.hotpulse.repository.MessageRepository;
import com.hotpulse.service.agent.AgentExecutionService;
import com.hotpulse.service.agent.AgentOrchestrator;
import com.hotpulse.sse.AgentSseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator agentOrchestrator;
    private final AgentExecutionService agentExecutionService;
    private final AgentSseService agentSseService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ExecutorService virtualThreadExecutor;

    @PostMapping("/query")
    public Result<AgentQueryResponse> query(@Valid @RequestBody AgentQueryRequest request) {
        // 解析或创建 conversationId
        Long conversationId = null;
        if (request.getConversationId() != null) {
            try {
                Long parsed = Long.parseLong(request.getConversationId());
                // verify conversation exists; if not, treat as null so we create a new one
                if (conversationRepository.existsById(parsed)) {
                    conversationId = parsed;
                }
            } catch (NumberFormatException ignored) {}
        }

        // 若未传入 conversationId，创建新会话
        if (conversationId == null) {
            Conversation conversation = new Conversation();
            String title = request.getQuery();
            conversation.setTitle(title.substring(0, Math.min(50, title.length())));
            conversation = conversationRepository.save(conversation);
            conversationId = conversation.getId();
        }

        // 保存用户消息到对话历史
        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(request.getQuery());
        messageRepository.save(userMessage);

        // 创建执行记录
        AgentExecution execution = agentExecutionService.create(request.getQuery(), conversationId);
        Long executionId = execution.getId();

        // 异步启动 Agent 管线（虚拟线程）
        final Long finalConversationId = conversationId;
        CompletableFuture.runAsync(
                () -> agentOrchestrator.execute(executionId, request.getQuery(), finalConversationId),
                virtualThreadExecutor
        );

        return Result.ok(new AgentQueryResponse(
                executionId.toString(),
                conversationId.toString(),
                "RUNNING"
        ));
    }

    @GetMapping(value = "/stream/{executionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String executionId) {
        return agentSseService.subscribe(executionId);
    }

    @GetMapping("/executions")
    public Result<Map<String, Object>> listExecutions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Page<AgentExecution> pageResult = agentExecutionService.listHistory(page, limit);
        return Result.ok(Map.of(
                "total", pageResult.getTotalElements(),
                "items", pageResult.getContent()
        ));
    }

    @GetMapping("/conversations")
    public Result<Map<String, Object>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int limit) {
        Page<Conversation> pageResult = conversationRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page - 1, limit));
        return Result.ok(Map.of(
                "total", pageResult.getTotalElements(),
                "items", pageResult.getContent()
        ));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<Message>> getConversationMessages(@PathVariable Long conversationId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return Result.ok(messages);
    }
}
