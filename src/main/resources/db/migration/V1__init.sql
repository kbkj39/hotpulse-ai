-- V1__init.sql: 初始化所有表结构

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 信息源表
CREATE TABLE sources (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    type             VARCHAR(20)  NOT NULL CHECK (type IN ('RSS', 'HTML', 'API')),
    base_url         VARCHAR(2048) NOT NULL,
    reputation_score DOUBLE PRECISION DEFAULT 1.0,
    enabled          BOOLEAN DEFAULT TRUE,
    robots_allowed   BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 原始网页表
CREATE TABLE raw_pages (
    id             BIGSERIAL PRIMARY KEY,
    source_id      BIGINT NOT NULL REFERENCES sources(id),
    url            VARCHAR(2048) NOT NULL,
    canonical_url  VARCHAR(2048),
    fingerprint    VARCHAR(64) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'FETCHED', 'FAILED')),
    raw_content    TEXT,
    fetched_at     TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_raw_pages_fingerprint   ON raw_pages(fingerprint);
CREATE INDEX idx_raw_pages_canonical_url ON raw_pages(canonical_url);
CREATE INDEX idx_raw_pages_status        ON raw_pages(status);

-- 清洗后文档表
CREATE TABLE documents (
    id           BIGSERIAL PRIMARY KEY,
    raw_page_id  BIGINT REFERENCES raw_pages(id),
    title        VARCHAR(1024) NOT NULL,
    content      TEXT NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    summary      TEXT,
    author       VARCHAR(255),
    source_url   VARCHAR(2048),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_published_at ON documents(published_at);

-- 文本切片表
CREATE TABLE chunks (
    id           BIGSERIAL PRIMARY KEY,
    document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index  INTEGER NOT NULL,
    text         TEXT NOT NULL,
    token_count  INTEGER,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunks_document_id ON chunks(document_id);

-- 向量嵌入表 (PGVector)
CREATE TABLE embeddings (
    id         BIGSERIAL PRIMARY KEY,
    chunk_id   BIGINT NOT NULL REFERENCES chunks(id) ON DELETE CASCADE,
    model      VARCHAR(100) NOT NULL,
    embedding  vector(1536) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_embeddings_chunk_id ON embeddings(chunk_id);
CREATE INDEX idx_embeddings_ivfflat ON embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 热点条目表
CREATE TABLE hotspots (
    id                BIGSERIAL PRIMARY KEY,
    document_id       BIGINT NOT NULL REFERENCES documents(id),
    execution_id      BIGINT,
    truth_score       DOUBLE PRECISION,
    relevance_score   DOUBLE PRECISION,
    importance_score  DOUBLE PRECISION,
    hot_score         DOUBLE PRECISION,
    tags              TEXT,
    analysis_evidence TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hotspots_hot_score        ON hotspots(hot_score DESC);
CREATE INDEX idx_hotspots_importance_score ON hotspots(importance_score DESC);

-- 对话会话表
CREATE TABLE conversations (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 对话消息表
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content         TEXT NOT NULL,
    sources_json    TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);

-- Agent 执行记录表
CREATE TABLE agent_executions (
    id              BIGSERIAL PRIMARY KEY,
    query           TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'DONE', 'FAILED')),
    task_plan_json  TEXT,
    conversation_id BIGINT REFERENCES conversations(id),
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Agent 步骤执行记录表
CREATE TABLE agent_execution_steps (
    id           BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES agent_executions(id) ON DELETE CASCADE,
    agent_name   VARCHAR(100) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
    detail_json  TEXT,
    started_at   TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_execution_steps_execution_id ON agent_execution_steps(execution_id);

-- 每日财经日报表
CREATE TABLE daily_reports (
    id             BIGSERIAL PRIMARY KEY,
    report_date    DATE NOT NULL UNIQUE,
    content        TEXT NOT NULL,
    hotspot_count  INTEGER,
    generated_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
