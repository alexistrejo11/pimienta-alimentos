package io.github.alexistrejo.pimienta.pos.data.sync

// Decides which device catalog calls an edit needs. Unchanged sides are skipped.
internal data class ProductEditPlan(val rename: Boolean, val offer: Boolean)

// Blank or equal to the SKU means the scanner uses the SKU. The SKU itself is not stored again.
internal fun catalogBarcode(raw: String?, sku: String): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (trimmed.equals(sku, ignoreCase = true)) null else trimmed
}

// Code the cashier can scan: the barcode when one exists, otherwise the SKU.
internal fun scanCode(barcode: String?, sku: String): String =
    barcode?.trim()?.takeIf { it.isNotEmpty() } ?: sku

internal fun planProductEdit(
    originalName: String,
    originalPriceCentavos: Long,
    originalControlled: Boolean,
    originalBarcode: String?,
    sku: String,
    name: String,
    priceCentavos: Long,
    controlled: Boolean,
    barcode: String?,
): ProductEditPlan = ProductEditPlan(
    rename = name.trim() != originalName.trim() || catalogBarcode(barcode, sku) != catalogBarcode(originalBarcode, sku),
    offer = priceCentavos != originalPriceCentavos || controlled != originalControlled,
)

// Runs the planned calls in order and persists each success before the next call.
internal suspend fun applyPlannedProductEdit(
    plan: ProductEditPlan,
    rename: suspend () -> ProductDto,
    offer: suspend () -> ProductDto,
    persist: (ProductDto) -> Unit,
): ProductDto? {
    if (!plan.rename && !plan.offer) return null
    var latest: ProductDto? = null
    if (plan.rename) {
        latest = rename()
        persist(latest)
    }
    if (plan.offer) {
        latest = offer()
        persist(latest)
    }
    return latest
}
