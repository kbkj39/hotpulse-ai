package com.hotpulse.skill;

public record SkillResult<O>(
    String status,
    O data,
    String error,
    String traceId,
    long durationMs
) {
    public static <O> SkillResult<O> ok(O data, String traceId, long durationMs) {
        return new SkillResult<>("ok", data, null, traceId, durationMs);
    }

    public static <O> SkillResult<O> error(String error, String traceId, long durationMs) {
        return new SkillResult<>("error", null, error, traceId, durationMs);
    }

    public boolean isOk() {
        return "ok".equals(status);
    }
}
