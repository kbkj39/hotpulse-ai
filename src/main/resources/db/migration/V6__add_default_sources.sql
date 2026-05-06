-- V6__add_default_sources.sql
-- 为开发环境添加若干示例财经信息源

INSERT INTO sources (name, type, base_url, reputation_score, enabled, robots_allowed)
VALUES
  ('新浪财经', 'RSS', 'https://finance.sina.com.cn/rss.xml', 0.95, TRUE, TRUE),
  ('财新网',   'RSS', 'https://www.caixin.com/rss/', 0.95, TRUE, TRUE),
  ('东方财富', 'HTML', 'http://finance.eastmoney.com', 0.90, TRUE, TRUE),
  ('和讯网',   'HTML', 'http://www.hexun.com', 0.85, TRUE, TRUE),
  ('同花顺',   'API', 'https://openapi.iwencai.com', 0.90, TRUE, TRUE);

-- 如果希望在生产环境使用，请根据真实 RSS/API endpoint 与 robots 策略调整 base_url 与 robots_allowed。
