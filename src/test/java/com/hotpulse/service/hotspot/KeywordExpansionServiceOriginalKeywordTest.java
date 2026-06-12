package com.hotpulse.service.hotspot;

import com.hotpulse.skill.ExpandKeywordsSkill;
import com.hotpulse.skill.SkillResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeywordExpansionServiceOriginalKeywordTest {

    @Test
    void keepsOriginalKeywordsBeforeLlmExpansion() {
        ExpandKeywordsSkill skill = mock(ExpandKeywordsSkill.class);
        when(skill.execute(any())).thenReturn(
                SkillResult.ok(List.of("openai", "chatgpt", "大模型"), "trace", 100));
        KeywordExpansionService service = new KeywordExpansionService(skill);

        List<String> expanded = service.expand(List.of("opai"));

        assertThat(expanded).containsExactly("opai", "openai", "chatgpt", "大模型");
    }
}

