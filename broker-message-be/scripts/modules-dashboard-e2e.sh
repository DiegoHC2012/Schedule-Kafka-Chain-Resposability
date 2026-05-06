#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

if ! command -v curl >/dev/null 2>&1; then
  echo "Error: curl no esta instalado o no esta en PATH."
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "Error: jq es requerido para parsear respuestas JSON."
  exit 1
fi

SUFFIX="$(date +%s)"
EMAIL="dashboard-${SUFFIX}@example.com"
PRODUCT_1="sku-${SUFFIX}-1"
PRODUCT_2="sku-${SUFFIX}-2"

echo "[1/8] Creando producto demo ${PRODUCT_1}..."
curl -fsS -X POST "${BASE_URL}/productos" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON | jq .
{
  "productId": "${PRODUCT_1}",
  "name": "Laptop Demo",
  "availableQuantity": 10
}
JSON

echo "[2/8] Creando producto demo ${PRODUCT_2}..."
curl -fsS -X POST "${BASE_URL}/productos" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON | jq .
{
  "productId": "${PRODUCT_2}",
  "name": "Mouse Demo",
  "availableQuantity": 20
}
JSON

echo "[3/8] Creando orden demo en ${BASE_URL}/ordenes..."
ORDER_RESPONSE=$(curl -fsS -X POST "${BASE_URL}/ordenes" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON
{
  "customerEmail": "${EMAIL}",
  "totalAmount": 250.00,
  "products": [
    {"productId": "${PRODUCT_1}", "quantity": 1},
    {"productId": "${PRODUCT_2}", "quantity": 2}
  ]
}
JSON
)
echo "$ORDER_RESPONSE" | jq .
ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.orderId')

echo "[4/8] Actualizando orden a EN_PROCESO..."
STATUS_RESPONSE=$(curl -fsS -X PUT "${BASE_URL}/ordenes/estatus" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON
{
  "orderId": "${ORDER_ID}",
  "status": "EN_PROCESO"
}
JSON
)
echo "$STATUS_RESPONSE" | jq .

echo "[5/8] Registrando pago parcial (restante 150.00)..."
PARTIAL_PAYMENT=$(curl -fsS -X POST "${BASE_URL}/pagos" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON
{
  "orderId": "${ORDER_ID}",
  "customerEmail": "${EMAIL}",
  "amount": 100.00,
  "remainingBalance": 150.00
}
JSON
)
echo "$PARTIAL_PAYMENT" | jq .

echo "[6/8] Registrando pago final (restante 0.00)..."
FINAL_PAYMENT=$(curl -fsS -X POST "${BASE_URL}/pagos" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON
{
  "orderId": "${ORDER_ID}",
  "customerEmail": "${EMAIL}",
  "amount": 150.00,
  "remainingBalance": 0.00
}
JSON
)
echo "$FINAL_PAYMENT" | jq .

echo "[7/8] Esperando 12 segundos para que el scheduler marque el envio como ENVIADO..."
sleep 12

echo "[8/8] Verificando dashboard y registros relacionados..."
echo "\n--- Orden en dashboard ---"
curl -fsS "${BASE_URL}/api/dashboard/orders?size=20" | jq --arg oid "$ORDER_ID" '.[] | select(.id == $oid)'

echo "\n--- Catalogo de productos ---"
curl -fsS "${BASE_URL}/productos" | jq --arg p1 "$PRODUCT_1" --arg p2 "$PRODUCT_2" '[.[] | select(.productId == $p1 or .productId == $p2)]'

echo "\n--- Pagos del dashboard ---"
curl -fsS "${BASE_URL}/api/dashboard/payments?size=20" | jq --arg oid "$ORDER_ID" '[.[] | select(.orderId == $oid)]'

echo "\n--- Envio del dashboard ---"
curl -fsS "${BASE_URL}/api/dashboard/shipments?size=20" | jq --arg oid "$ORDER_ID" '.[] | select(.orderId == $oid)'

echo "\n--- Module stats ---"
curl -fsS "${BASE_URL}/api/dashboard/module-stats" | jq .

echo "\nFlujo completado. Abre ${BASE_URL} para revisar el dashboard visualmente."