# Seed debug y Room

## Fuente de verdad

`docs/technical/data/deprecated_product_data.json` conserva la data real de productos. No se
usa directamente como modelo de Room porque su esquema está deprecated.

La conversión reproducible está en:

```powershell
.\scripts\convert-legacy-products.ps1
```

Su salida es `app/src/main/assets/pos-training-bootstrap.json`.

La conversión actual:

- asigna IDs POS incrementales (`product-0001`, etc.);
- hace únicos los códigos duplicados agregando sufijos;
- conserva nombre, categoría, precio, costo y stock;
- usa `PIECE` para todos los productos de esta versión;
- marca los códigos `CAF-` como `NOT_CONTROLLED`;
- marca los códigos comerciales numéricos como `CONTROLLED`;
- excluye imágenes Base64 del seed inicial.

## Cómo entra a Room

La aplicación debug leerá el asset y ejecutará un importador que:

1. valida `schemaVersion` y los campos obligatorios;
2. abre una transacción Room;
3. inserta sede, configuración, categorías y productos;
4. guarda una marca de bootstrap aplicada (`snapshotId`);
5. no vuelve a insertar el mismo snapshot.

Room crea y administra el archivo SQLite. No se necesita crear SQLite a mano ni
mantener queries SQL manuales para el seed. Las migraciones futuras sí serán
versionadas por Room cuando cambie el esquema.
