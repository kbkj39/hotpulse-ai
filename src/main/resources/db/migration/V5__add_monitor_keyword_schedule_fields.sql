ALTER TABLE monitor_keywords
    ADD COLUMN IF NOT EXISTS crawl_interval_hours INT       DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS last_crawled_at      TIMESTAMP WITH TIME ZONE DEFAULT NULL;
