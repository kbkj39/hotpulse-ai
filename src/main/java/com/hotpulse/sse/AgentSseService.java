package com.hotpulse.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.dto.AgentStepEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSseService {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    /**
     * 事件缓冲区：前端订阅前产生的事件暂存此处，订阅时回放。
     * 解决后端 Agent 流程在前端建立 SSE 连接前已完成推送的竞争条件。
     */
    private final ConcurrentHashMap<String, List<AgentStepEvent>> eventBuffers = new ConcurrentHashMap<>();
    /** 标记已由后端主动 complete 的 executionId，用于订阅时立即关闭连接 */
    private final ConcurrentHashMap<String, Boolean> completedBeforeSubscribe = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseEmitter subscribe(String executionId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutes timeout

        // 回放已缓冲的事件（解决订阅前已推送的竞争条件）
        List<AgentStepEvent> buffered = eventBuffers.getOrDefault(executionId, List.of());
        for (AgentStepEvent event : buffered) {
            try {
                String json = objectMapper.writeValueAsString(event);
                emitter.send(SseEmitter.event().data(json));
            } catch (IOException e) {
                log.warn("Failed to replay buffered SSE event for executionId={}", executionId);
            }
        }

        // 若后端在订阅前已完成，立即关闭连接并清理
        if (completedBeforeSubscribe.remove(executionId) != null) {
            eventBuffers.remove(executionId);
            emitter.complete();
            return emitter;
        }

        emitters.put(executionId, emitter);
        emitter.onCompletion(() -> emitters.remove(executionId));
        emitter.onTimeout(() -> {
            emitters.remove(executionId);
            emitter.complete();
        });
        emitter.onError(e -> emitters.remove(executionId));
        return emitter;
    }

    public void broadcast(String executionId, AgentStepEvent event) {
        // 先缓冲，再推送（确保订阅者可以回放）
        eventBuffers.computeIfAbsent(executionId, k -> new CopyOnWriteArrayList<>()).add(event);

        SseEmitter emitter = emitters.get(executionId);
        if (emitter == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            log.warn("SSE send failed for executionId={}, removing emitter", executionId);
            emitters.remove(executionId);
        }
    }

    public void complete(String executionId) {
        SseEmitter emitter = emitters.remove(executionId);
        if (emitter != null) {
            eventBuffers.remove(executionId);
            emitter.complete();
        } else {
            // 前端尚未订阅，标记为已完成；订阅到来时立即关闭
            completedBeforeSubscribe.put(executionId, Boolean.TRUE);
        }
    }
}
