package com.hotpulse.service.hotspot;

import com.hotpulse.skill.ExpandKeywordsSkill;
import com.hotpulse.skill.SkillResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KeywordExpansionServiceTest {

    @Test
    void usesLlmExpansionWhenSkillSucceeds() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenReturn(
                SkillResult.ok(List.of("比特币", "bitcoin", "btc", "加密货币", "区块链", "以太坊"), "trace", 100));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(List.of("比特币"));

        assertThat(expanded).containsExactly("比特币", "bitcoin", "btc", "加密货币", "区块链", "以太坊");
    }

    @Test
    void fallsBackToStaticAliasesWhenLlmFails() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenReturn(
                SkillResult.error("LLM timeout", "trace", 20000));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(List.of("gpt"));

        assertThat(expanded)
                .containsExactly("gpt", "chatgpt", "openai", "大模型", "生成式ai", "人工智能");
    }

    @Test
    void fallsBackToStaticAliasesWhenLlmThrows() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenThrow(new RuntimeException("connection refused"));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(List.of("比特币"));

        assertThat(expanded).contains("比特币", "bitcoin", "btc");
    }

    @Test
    void splitsAndDeduplicatesKeywordsCaseInsensitively() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenReturn(
                SkillResult.ok(List.of("GPT", "chatgpt", "openai", "大模型", "生成式ai", "人工智能"), "trace", 100));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(List.of("GPT，openai gpt"));

        assertThat(expanded)
                .containsExactly("GPT", "openai", "chatgpt", "大模型", "生成式ai", "人工智能");
    }

    @Test
    void returnsOnlyAliasesNotOriginalKeywords() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenReturn(
                SkillResult.ok(List.of("gpt", "chatgpt", "openai", "大模型", "生成式ai", "人工智能"), "trace", 100));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> aliasesOnly = service.aliasesOnly(List.of("gpt"));

        assertThat(aliasesOnly)
                .containsExactly("chatgpt", "openai", "大模型", "生成式ai", "人工智能");
    }

    @Test
    void limitsExpandedKeywords() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenReturn(
                SkillResult.ok(List.of("gpt", "英伟达", "nvidia", "比特币", "btc", "美联储", "fed", "降息", "加息", "a", "b", "c", "d"), "trace", 100));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(List.of("gpt 英伟达 比特币 美联储"));

        assertThat(expanded).hasSizeLessThanOrEqualTo(12);
    }

    @Test
    void returnsEmptyForNullInput() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(null);

        assertThat(expanded).isEmpty();
    }
}
