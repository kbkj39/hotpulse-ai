-- V7__source_cascade_delete.sql
-- 为信息源删除添加级联约束：
--   删除 source 时自动级联删除 raw_pages → documents → chunks/embeddings → hotspots

-- raw_pages.source_id: NOT NULL → ON DELETE CASCADE
ALTER TABLE raw_pages
    DROP CONSTRAINT IF EXISTS raw_pages_source_id_fkey,
    ADD CONSTRAINT raw_pages_source_id_fkey
        FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE;

-- documents.raw_page_id: nullable → ON DELETE CASCADE（raw_page 删除时一并删文档）
ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS documents_raw_page_id_fkey,
    ADD CONSTRAINT documents_raw_page_id_fkey
        FOREIGN KEY (raw_page_id) REFERENCES raw_pages(id) ON DELETE CASCADE;

-- hotspots.document_id: NOT NULL → ON DELETE CASCADE（文档删除时一并删热点）
ALTER TABLE hotspots
    DROP CONSTRAINT IF EXISTS hotspots_document_id_fkey,
    ADD CONSTRAINT hotspots_document_id_fkey
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
