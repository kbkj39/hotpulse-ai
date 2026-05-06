-- V3: 替换无效的 bilibili RSS 信息源，添加实际可用的财经/科技新闻 RSS 源

-- 先清理依赖 sources 的下游数据（外键级联）
DELETE FROM raw_pages;

-- 清理所有测试期间通过 API 写入的重复/无效信息源
DELETE FROM sources;

-- 重置 ID 序列，使新插入从 1 开始
ALTER SEQUENCE sources_id_seq RESTART WITH 1;

-- 添加真实可用的中文财经/科技 RSS 信息源
INSERT INTO sources (name, type, base_url, reputation_score, enabled, robots_allowed) VALUES
    ('36氪',         'RSS', 'https://36kr.com/feed',                   0.9, true, true),
    ('虎嗅',         'RSS', 'https://www.huxiu.com/rss/0.xml',         0.9, true, true),
    ('IT之家',       'RSS', 'https://www.ithome.com/rss/',             0.8, true, true),
    ('少数派',       'RSS', 'https://sspai.com/feed',                  0.8, true, true),
    ('澎湃新闻科技', 'RSS', 'https://www.thepaper.cn/rss_tech.xml',   0.9, true, true);
