# broker-message-be

## Prueba E2E de reintentos

El script scripts/retry-e2e.sh publica un evento en Kafka y monitorea el estado del registro en retry_jobs para validar:

- incremento de attempt_count
- reprogramacion en next_retry_at con backoff
- cambio a FAILED al agotar max_attempts
- insercion en la tabla especifica del dominio

### Uso

Desde la carpeta broker-message-be:

- payment (8 muestras):
  ./scripts/retry-e2e.sh payment

- order (10 muestras):
  ./scripts/retry-e2e.sh order 10

- product (6 muestras):
  ./scripts/retry-e2e.sh product 6

Notas:

- El script levanta postgres, zookeeper y kafka con docker compose.
- Si el endpoint destino no existe, la prueba mostrara reintentos y luego FAILED.
- Si quieres validar exito, levanta un mock HTTP en el endpoint configurado para ese dominio.

## Flujo E2E para dashboard de modulos

El script scripts/modules-dashboard-e2e.sh ejecuta un flujo completo para poblar el dashboard de ordenes, pagos y envios:

- crea productos en el catalogo
- crea una orden
- cambia su estatus a EN_PROCESO
- registra un pago parcial
- registra un pago final
- espera al scheduler de envios
- consulta los endpoints del dashboard y el catalogo para verificar orden, productos, pagos y envio

## Catalogo de productos

El endpoint /productos permite registrar y listar los productos que luego pueden usarse en las ordenes.

### Endpoints

- POST /productos
- GET /productos

Ejemplo de alta:

```json
{
  "productId": "sku-001",
  "name": "Laptop Pro",
  "availableQuantity": 15
}
```

Las ordenes aceptan productos con formato corto basado en catalogo:

```json
{
  "products": [
    { "productId": "sku-001", "quantity": 1 }
  ]
}
```

### Requisitos

- La aplicacion debe estar corriendo en http://localhost:8080 o en la URL definida por BASE_URL.
- jq debe estar instalado para inspeccionar respuestas JSON.

### Uso

Desde la carpeta broker-message-be:

- flujo por defecto:
  ./scripts/modules-dashboard-e2e.sh

- con otra URL base:
  BASE_URL=http://localhost:8080 ./scripts/modules-dashboard-e2e.sh
