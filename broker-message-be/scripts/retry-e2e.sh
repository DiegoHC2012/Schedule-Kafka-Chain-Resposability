#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:-payment}"
ITERATIONS="${2:-8}"

case "$DOMAIN" in
  payment)
    TOPIC="payments_retry_jobs"
    ENDPOINT_DEFAULT="http://localhost:8081/api/payments"
    FAILED_TABLE="payments_retry_jobs"
    ;;
  order)
    TOPIC="order_retry_jobs"
    ENDPOINT_DEFAULT="http://localhost:8082/api/orders"
    FAILED_TABLE="order_retry_jobs"
    ;;
  product)
    TOPIC="product_retry_jobs"
    ENDPOINT_DEFAULT="http://localhost:8083/api/products"
    FAILED_TABLE="product_retry_jobs"
    ;;
  *)
    echo "Uso: $0 [payment|order|product] [iteraciones_monitor]"
    exit 1
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker no esta instalado o no esta en PATH."
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Error: docker compose no esta disponible."
  exit 1
fi

echo "[1/5] Levantando infraestructura (postgres, zookeeper, kafka)..."
docker compose up -d postgres zookeeper kafka

echo "[2/5] Esperando que kafka y postgres esten disponibles..."
for _ in {1..40}; do
  if docker ps --format '{{.Names}}' | grep -q '^broker-kafka$' && \
     docker ps --format '{{.Names}}' | grep -q '^broker-postgres$'; then
    break
  fi
  sleep 2
done

if ! docker ps --format '{{.Names}}' | grep -q '^broker-kafka$'; then
  echo "Error: broker-kafka no esta en ejecucion."
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q '^broker-postgres$'; then
  echo "Error: broker-postgres no esta en ejecucion."
  exit 1
fi

echo "[3/5] Verificando tabla retry_jobs..."
docker exec broker-postgres psql -U postgres -d broker_db -c "SELECT COUNT(*) AS total_retry_jobs FROM retry_jobs;" >/dev/null

TEST_ID="e2e-${DOMAIN}-$(date +%s)"
PAYLOAD=$(cat <<EOF
{"data":{"id":"$TEST_ID","name":"${DOMAIN}-demo","description":"evento e2e","price":100.0,"quantity":1,"image":"img.jpg","category":"demo","subcategory":"demo","brand":"demo"}}
EOF
)

echo "[4/5] Publicando mensaje en topico ${TOPIC} con id ${TEST_ID}..."
printf '%s\n' "$PAYLOAD" | docker exec -i broker-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic "$TOPIC" >/dev/null

echo "\n[5/5] Monitoreando evolucion de reintentos (${ITERATIONS} muestras, cada 10s)..."
echo "       Si el endpoint destino no existe, veras attempt_count subir y luego FAILED al llegar a max_attempts."

for i in $(seq 1 "$ITERATIONS"); do
  echo "\n===== Muestra $i/$ITERATIONS - $(date '+%Y-%m-%d %H:%M:%S') ====="
  docker exec broker-postgres psql -U postgres -d broker_db -c "
    SELECT id,
           topic,
           status,
           attempt_count,
           max_attempts,
           to_char(next_retry_at, 'YYYY-MM-DD HH24:MI:SS') AS next_retry_at,
           step_a_status,
           step_b_status,
           step_c_status,
           LEFT(COALESCE(error_message, ''), 120) AS error_message
    FROM retry_jobs
    WHERE payload LIKE '%\"id\":\"${TEST_ID}\"%'
    ORDER BY created_at DESC
    LIMIT 1;
  "

  docker exec broker-postgres psql -U postgres -d broker_db -c "
    SELECT COUNT(*) AS failed_rows_in_${FAILED_TABLE}
    FROM ${FAILED_TABLE} frj
    JOIN retry_jobs rj ON rj.id = frj.retry_job_id
    WHERE rj.payload LIKE '%\"id\":\"${TEST_ID}\"%';
  "

  if [ "$i" -lt "$ITERATIONS" ]; then
    sleep 10
  fi
done

echo "\nPrueba finalizada."
echo "Sugerencia: si quieres forzar exito, levanta un mock HTTP en ${ENDPOINT_DEFAULT}."
