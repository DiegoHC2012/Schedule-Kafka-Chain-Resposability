-- ============================================================
-- V4: Avoid duplicate shipment notifications
-- ============================================================

ALTER TABLE envios
    ADD COLUMN IF NOT EXISTS notification_sent BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE envios
SET notification_sent = TRUE
WHERE status = 'ENVIADO';

CREATE INDEX IF NOT EXISTS idx_envios_status_notification_sent
    ON envios (status, notification_sent);