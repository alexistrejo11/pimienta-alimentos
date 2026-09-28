package io.github.alexistrejo.pimienta.pos.domain

import java.text.Collator
import java.util.Locale

// Spanish alphabetical order that ignores case and accents ("bebidas" next to "Bebidas", "Panadería" next to "Panaderia").
private val categoryCollator: Collator = Collator.getInstance(Locale.forLanguageTag("es-MX")).apply {
    strength = Collator.PRIMARY
}

// Returns non-blank category names without case-insensitive duplicates, sorted alphabetically.
fun sortedCategoryNames(names: Iterable<String>): List<String> =
    names.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .sortedWith(categoryCollator)
