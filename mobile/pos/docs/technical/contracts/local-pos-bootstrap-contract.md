# Contrato local `pos-bootstrap`

Este contrato permite avanzar con la aplicación sin esperar el contrato del
backend. El backend podrá adaptarse después mediante un mapper, sin cambiar el
modelo local ni la experiencia de venta.

## Reglas locales

- Los importes se representan como texto decimal en JSON y se convertirán a
  una representación exacta dentro del dominio; nunca `Float` o `Double`.
- `price` es el precio efectivo que verá el POS para esa sede.
- `saleCategory` es una categoría de venta simple y pertenece al snapshot.
- `available` indica si puede venderse; `stock` no decide por sí solo la
  disponibilidad de productos `NOT_CONTROLLED`.
- En esta versión todas las ventas usan unidad `PIECE`.
- Los PIN incluidos son exclusivamente de debug y no son credenciales reales.
- El snapshot debug vive en `src/debug/assets` y nunca se empaqueta en release.

## Forma mínima

```json
{
  "schemaVersion": 1,
  "kind": "pos-bootstrap",
  "site": { "id": "...", "name": "...", "currency": "MXN" },
  "users": [],
  "products": [],
  "openAmountCategories": []
}
```

## Decisiones pospuestas

El contrato local no fija todavía el endpoint remoto, autenticación,
sincronización, hardware ni mapeo con las categorías del backend.
