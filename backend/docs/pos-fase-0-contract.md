# Contrato POS: Fases 0 y 1

Estado: estricto y vigente desde 2026-09-16.

Estas reglas son la base de la implementación Backend/Web. El contrato
cross-app canónico se mantiene en `mobile/pos/docs/integration/03-fase-0-contrato-cerrado.md`.

## Reglas obligatorias

- El feed descendente usa una secuencia monotónica explícita por sede/HQ. Los
  cursores basados en `updatedAt`, `Instant` o la hora local no son el contrato
  objetivo.
- `OPEN_AMOUNT` requiere `allowOpenProducts=true`, una categoría permitida,
  importe positivo y cantidad exactamente igual a uno.
- Una línea `OPEN_AMOUNT` no requiere PIN. El cajero captura categoría e
  importe; `authorizedByOperatorId` y `authorizedAt` pueden ir nulos.
- Una línea `OPEN_AMOUNT` usa `productId=null`, `rawBarcode=null`,
  `stockPolicy=NOT_CONTROLLED` y nunca genera movimiento de inventario.
- El barcode desconocido conserva el flujo `PENDING_CATALOG`. No se convierte
  automáticamente en `OPEN_AMOUNT` ni pierde la evidencia del barcode.
- Las categorías permitidas se normalizan eliminando espacios exteriores,
  valores vacíos y duplicados sin distinguir mayúsculas/minúsculas. Si el
  monto abierto está habilitado debe existir al menos una categoría.
