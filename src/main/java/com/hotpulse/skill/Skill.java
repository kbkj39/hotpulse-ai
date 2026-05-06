package com.hotpulse.skill;

import java.time.Duration;

public interface Skill<I, O> {
    String name();
    Duration timeout();
    SkillResult<O> execute(I input);
}
