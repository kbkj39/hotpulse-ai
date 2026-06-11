-- 每日日报增加生成状态字段
-- 历史记录视为已成功生成（status=READY），error_message 留空
ALTER TABLE daily_reports
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY',
    ADD COLUMN error_message TEXT;
