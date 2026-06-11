package com.hotpulse.entity;

// 每日日报的生成状态：
//   PENDING    - 已创建记录，等待生成
//   GENERATING - 正在调用 LLM 生成
//   READY      - 生成成功，content 可用
//   EMPTY      - 该报告日无热点数据，content 为空
//   FAILED     - 生成失败，error_message 记录原因，下次 cron 会自动重试
public enum DailyReportStatus {
    PENDING,
    GENERATING,
    READY,
    EMPTY,
    FAILED
}
