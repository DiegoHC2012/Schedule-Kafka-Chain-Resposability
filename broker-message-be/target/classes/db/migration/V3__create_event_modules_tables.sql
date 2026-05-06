-- ============================================================
-- V3: Create payments, orders and shipments tables
-- ============================================================

CREATE TABLE IF NOT EXISTS ordenes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_email      VARCHAR(255)    NOT NULL,
    total_amount        NUMERIC(14, 2)  NOT NULL,
    remaining_balance   NUMERIC(14, 2)  NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ordenes_status ON ordenes (status);

CREATE TABLE IF NOT EXISTS orden_productos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL REFERENCES ordenes(id) ON DELETE CASCADE,
    product_id      VARCHAR(100)    NOT NULL,
    product_name    VARCHAR(255)    NOT NULL,
    quantity        INTEGER         NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orden_productos_order_id ON orden_productos (order_id);
CREATE INDEX IF NOT EXISTS idx_orden_productos_product_id ON orden_productos (product_id);

CREATE TABLE IF NOT EXISTS pagos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL REFERENCES ordenes(id),
    customer_email      VARCHAR(255)    NOT NULL,
    amount              NUMERIC(14, 2)  NOT NULL,
    remaining_balance   NUMERIC(14, 2)  NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pagos_order_id ON pagos (order_id);

CREATE TABLE IF NOT EXISTS envios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL UNIQUE REFERENCES ordenes(id),
    payment_id      UUID                REFERENCES pagos(id),
    customer_email  VARCHAR(255)    NOT NULL,
    status          VARCHAR(30)     NOT NULL,
    shipped_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_envios_status ON envios (status);