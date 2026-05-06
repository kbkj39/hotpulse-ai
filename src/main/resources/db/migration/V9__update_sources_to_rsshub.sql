-- V9__update_sources_to_rsshub.sql
-- 将信息源替换为自部署 RSSHub 路由（容器内访问 http://rsshub:1200）
-- 已验证可用的路由：36kr/newsflashes, wallstreetcn/news/global, huxiu/article,
--                   jin10/flash_newest, jin10/news, cls/telegraph
-- sina/eastmoney/caixin 路由被源站屏蔽，替换为可用的金十/财联社路由

-- 更新现有源地址（将旧 HTML 源替换为 RSSHub 高质量路由）
UPDATE sources SET type='RSS', base_url='http://rsshub:1200/jin10/flash_newest'           WHERE name='新浪财经';
UPDATE sources SET name='36氪', type='RSS', base_url='http://rsshub:1200/36kr/newsflashes' WHERE name='财新网';
UPDATE sources SET name='金十新闻', type='RSS', base_url='http://rsshub:1200/jin10/news'  WHERE name='东方财富';
UPDATE sources SET enabled=false                                                            WHERE name='和讯网';
UPDATE sources SET name='财联社', type='RSS', base_url='http://rsshub:1200/cls/telegraph' WHERE name='新浪网';

-- 新增优质财经 RSS 源（幂等，已存在则跳过）
INSERT INTO sources (name, type, base_url, reputation_score, enabled, robots_allowed)
SELECT name, type, base_url, reputation_score, enabled, robots_allowed FROM (VALUES
  ('36氪快讯',   'RSS', 'http://rsshub:1200/36kr/newsflashes',          0.90, TRUE, TRUE),
  ('华尔街见闻', 'RSS', 'http://rsshub:1200/wallstreetcn/news/global',  0.92, TRUE, TRUE),
  ('虎嗅财经',   'RSS', 'http://rsshub:1200/huxiu/article',             0.88, TRUE, TRUE)
) AS v(name, type, base_url, reputation_score, enabled, robots_allowed)
WHERE NOT EXISTS (SELECT 1 FROM sources s WHERE s.name = v.name);
