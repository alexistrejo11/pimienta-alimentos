package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import java.math.BigDecimal

// Maps Device API catalog DTOs onto Room rows used by the sale UI.
internal fun ProductDto.toProductEntity() = ProductEntity(
    id,
    sku,
    barcode,
    name,
    saleCategory,
    unit,
    BigDecimal.valueOf(priceCentavos, 2).toPlainString(),
    BigDecimal.valueOf(costCentavos, 2).toPlainString(),
    available,
    stockQuantity.toString(),
    stockMinQuantity.toString(),
    stockPolicy,
    negativeStockLimit,
)

// Builds a training-only catalog row. Ids never leave the scratch sandbox database.
internal fun trainingProductEntity(
    name: String,
    saleCategory: String,
    salePriceCentavos: Long,
    barcode: String?,
    controlledStock: Boolean,
    id: String,
): ProductEntity = ProductDto(
    id = id,
    sku = "TRN-${id.take(8)}",
    barcode = barcode,
    name = name,
    saleCategory = saleCategory,
    unit = "PIECE",
    priceCentavos = salePriceCentavos,
    costCentavos = 0,
    available = true,
    stockQuantity = 0,
    stockMinQuantity = 0,
    stockPolicy = if (controlledStock) "CONTROLLED" else "NOT_CONTROLLED",
).toProductEntity()

internal fun OperatorDto.toUser() = LocalUserEntity(id, displayName, LocalUserEntity.normalizeRole(role), pinHash, active)

internal fun SiteDto.toSite() = SiteEntity(id, name, address, currency)
