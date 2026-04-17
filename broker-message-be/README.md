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
