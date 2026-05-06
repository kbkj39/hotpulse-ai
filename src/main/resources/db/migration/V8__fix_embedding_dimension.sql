-- V8__fix_embedding_dimension.sql
-- 无需修改 DB 列维度：通过 Spring AI 配置 text-embedding-3-large 的 dimensions=1536
-- 使输出向量与现有 vector(1536) 列匹配，ivfflat 索引支持上限为 2000 维。
-- 清空旧的可能存在维度不匹配的 embedding 数据，确保数据一致性
TRUNCATE TABLE embeddings;
