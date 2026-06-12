package com.hotpulse.service.hotspot;

import com.hotpulse.skill.ExpandKeywordsSkill;
import com.hotpulse.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordExpansionService {

    private static final int MAX_KEYWORDS = 12;

    private final ExpandKeywordsSkill expandKeywordsSkill;

    private static final Map<String, List<String>> ALIASES = Map.ofEntries(
            Map.entry("gpt", List.of("gpt", "chatgpt", "openai", "大模型", "生成式ai", "人工智能")),
            Map.entry("chatgpt", List.of("chatgpt", "gpt", "openai", "大模型", "生成式ai")),
            Map.entry("openai", List.of("openai", "chatgpt", "gpt", "大模型")),
            Map.entry("ai", List.of("ai", "人工智能", "大模型", "生成式ai")),
            Map.entry("人工智能", List.of("人工智能", "ai", "大模型", "生成式ai")),
            Map.entry("大模型", List.of("大模型", "ai", "人工智能", "生成式ai")),
            Map.entry("英伟达", List.of("英伟达", "nvidia", "nvda", "黄仁勋", "gpu", "ai芯片")),
            Map.entry("nvidia", List.of("nvidia", "英伟达", "nvda", "黄仁勋", "gpu", "ai芯片")),
            Map.entry("nvda", List.of("nvda", "nvidia", "英伟达", "gpu", "ai芯片")),
            Map.entry("比特币", List.of("比特币", "bitcoin", "btc", "加密货币")),
            Map.entry("bitcoin", List.of("bitcoin", "比特币", "btc", "加密货币")),
            Map.entry("btc", List.of("btc", "bitcoin", "比特币", "加密货币")),
            Map.entry("美联储", List.of("美联储", "fed", "鲍威尔", "降息", "加息")),
            Map.entry("fed", List.of("fed", "美联储", "鲍威尔", "降息", "加息"))
    );

    public List<String> expand(List<String> keywords) {
        List<String> normalized = normalize(keywords);
        if (normalized.isEmpty()) {
            return List.of();
        }

        // 优先使用 LLM 动态扩展
        try {
            SkillResult<List<String>> result = expandKeywordsSkill.execute(
                    new ExpandKeywordsSkill.Input(normalized, MAX_KEYWORDS));
            if (result.isOk() && result.data() != null && !result.data().isEmpty()) {
                log.debug("LLM expanded keywords: {} -> {}", normalized, result.data());
                LinkedHashMap<String, String> merged = new LinkedHashMap<>();
                normalized.forEach(keyword -> add(merged, keyword));
                result.data().forEach(keyword -> add(merged, keyword));
                return merged.values().stream()
                        .limit(MAX_KEYWORDS)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("LLM keyword expansion failed, falling back to static aliases: {}", e.getMessage());
        }

        // 回退到硬编码别名表
        return expandStatic(normalized);
    }

    public List<String> aliasesOnly(List<String> keywords) {
        LinkedHashMap<String, String> originals = new LinkedHashMap<>();
        normalize(keywords).forEach(keyword -> add(originals, keyword));

        return expand(keywords).stream()
                .filter(keyword -> !originals.containsKey(keyword.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<String> expandStatic(List<String> normalized) {
        LinkedHashMap<String, String> expanded = new LinkedHashMap<>();

        for (String keyword : normalized) {
            add(expanded, keyword);
            ALIASES.getOrDefault(keyword.toLowerCase(Locale.ROOT), List.of())
                    .forEach(alias -> add(expanded, alias));
            if (expanded.size() >= MAX_KEYWORDS) {
                break;
            }
        }

        return expanded.values().stream()
                .limit(MAX_KEYWORDS)
                .toList();
    }

    private List<String> normalize(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            for (String part : keyword.split("[\\s,，、;；]+")) {
                String trimmed = part.trim();
                if (!trimmed.isBlank()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }

    private void add(LinkedHashMap<String, String> keywords, String keyword) {
        if (keyword == null || keyword.isBlank() || keywords.size() >= MAX_KEYWORDS) {
            return;
        }
        String trimmed = keyword.trim();
        keywords.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
    }
}
