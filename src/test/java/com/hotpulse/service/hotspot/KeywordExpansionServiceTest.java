package com.hotpulse.service.hotspot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExpansionServiceTest {

    private final KeywordExpansionService keywordExpansionService = new KeywordExpansionService();

    @Test
    void expandsKnownKeywordAliasesInStableOrder() {
        List<String> expanded = keywordExpansionService.expand(List.of("gpt"));

        assertThat(expanded)
                .containsExactly("gpt", "chatgpt", "openai", "大模型", "生成式ai", "人工智能");
    }

    @Test
    void splitsAndDeduplicatesKeywordsCaseInsensitively() {
        List<String> expanded = keywordExpansionService.expand(List.of("GPT，openai gpt"));

        assertThat(expanded)
                .containsExactly("GPT", "chatgpt", "openai", "大模型", "生成式ai", "人工智能");
    }

    @Test
    void returnsOnlyAliasesNotOriginalKeywords() {
        List<String> aliasesOnly = keywordExpansionService.aliasesOnly(List.of("gpt"));

        assertThat(aliasesOnly)
                .containsExactly("chatgpt", "openai", "大模型", "生成式ai", "人工智能");
    }

    @Test
    void limitsExpandedKeywords() {
        List<String> expanded = keywordExpansionService.expand(List.of("gpt 英伟达 比特币 美联储"));

        assertThat(expanded).hasSizeLessThanOrEqualTo(12);
    }
}
