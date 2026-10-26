package io.github.alexistrejo.pimienta.pos.domain

// Sale line names must fit pos_sale_lines.product_name.
internal const val OPEN_AMOUNT_PRODUCT_NAME_MAX_LENGTH = 255

// Builds the open-amount product name shared by the dialog, the cart, and sale confirmation.
internal fun formatOpenAmountProductName(category: String, concept: String = ""): String {
    val cleanCategory = category.trim()
    val cleanConcept = concept.trim().replace(Regex("\\s+"), " ")
    val prefix = "Monto Abierto · $cleanCategory"
    if (cleanConcept.isEmpty()) {
        return prefix.take(OPEN_AMOUNT_PRODUCT_NAME_MAX_LENGTH)
    }
    val opening = "$prefix ("
    val maxConceptLength = OPEN_AMOUNT_PRODUCT_NAME_MAX_LENGTH - opening.length - 1
    if (maxConceptLength <= 0) {
        return prefix.take(OPEN_AMOUNT_PRODUCT_NAME_MAX_LENGTH)
    }
    return "$opening${cleanConcept.take(maxConceptLength)})"
}

// True when a stored open-amount name is the generated label for this category.
internal fun openAmountNameMatchesCategory(name: String, category: String): Boolean {
    if (name.length > OPEN_AMOUNT_PRODUCT_NAME_MAX_LENGTH) return false
    val prefix = "Monto Abierto · ${category.trim()}"
    if (name == prefix) return true
    val opening = "$prefix ("
    if (!name.startsWith(opening) || !name.endsWith(")")) return false
    return name.removePrefix(opening).removeSuffix(")").isNotBlank()
}
