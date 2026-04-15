-- ============================================================
-- V1: Create retry tables
-- ============================================================

CREATE TABLE IF NOT EXISTS retry_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic           VARCHAR(100)  NOT NULL,
    payload         TEXT          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    step_a_status   VARCHAR(20),
    step_b_status   VARCHAR(20),
    step_c_status   VARCHAR(20),
    error_message   TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_retry_jobs_status  ON retry_jobs (status);
CREATE INDEX IF NOT EXISTS idx_retry_jobs_topic   ON retry_jobs (topic);

-- ============================================================

CREATE TABLE IF NOT EXISTS payments_retry_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retry_job_id    UUID          NOT NULL REFERENCES retry_jobs(id),
    payload         TEXT          NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_retry_jobs_retry_job_id ON payments_retry_jobs (retry_job_id);

-- ============================================================

CREATE TABLE IF NOT EXISTS order_retry_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retry_job_id    UUID          NOT NULL REFERENCES retry_jobs(id),
    payload         TEXT          NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_retry_jobs_retry_job_id ON order_retry_jobs (retry_job_id);

-- ============================================================

CREATE TABLE IF NOT EXISTS product_retry_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retry_job_id    UUID          NOT NULL REFERENCES retry_jobs(id),
    payload         TEXT          NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_retry_jobs_retry_job_id ON product_retry_jobs (retry_job_id);
