-- ============================================================
-- V2: Add retry policy columns to retry_jobs
-- ============================================================

ALTER TABLE retry_jobs
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_attempts INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP NOT NULL DEFAULT NOW();

UPDATE retry_jobs
SET attempt_count = COALESCE(attempt_count, 0),
    max_attempts = COALESCE(max_attempts, 3),
    next_retry_at = COALESCE(next_retry_at, NOW());

CREATE INDEX IF NOT EXISTS idx_retry_jobs_next_retry_at ON retry_jobs (next_retry_at);
CREATE INDEX IF NOT EXISTS idx_retry_jobs_status_next_retry_at ON retry_jobs (status, next_retry_at);
