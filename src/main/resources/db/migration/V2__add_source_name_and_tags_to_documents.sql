-- V2: add source_name and tags_json to documents table
-- source_name stores the name of the source (e.g. "CoinDesk"), populated at ingest time
-- tags_json stores the AI-generated tags as a JSON array, populated at ingest time

ALTER TABLE documents ADD COLUMN IF NOT EXISTS source_name VARCHAR(255);
ALTER TABLE documents ADD COLUMN IF NOT EXISTS tags_json TEXT;
