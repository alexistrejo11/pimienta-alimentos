package io.github.alexistrejo.pimienta.pos

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alexistrejo.pimienta.pos.data.local.entity.*
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.domain.*
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.printerStatusPresentation
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Lets portrait users switch between catalog and cart while preserving one draft sale.
@Composable
internal fun PortraitTabs(selected: PortraitPanel, itemCount: Int, onSelect: (PortraitPanel) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosButton("Catálogo", { onSelect(PortraitPanel.CATALOG) }, selected == PortraitPanel.CATALOG, modifier = Modifier.weight(1f))
        PosButton("Carrito · $itemCount", { onSelect(PortraitPanel.CART) }, selected == PortraitPanel.CART, modifier = Modifier.weight(1f))
    }
}


// Shows searchable catalog products as flat touch targets rather than elevated Material cards.
@Composable
internal fun CatalogPanel(
    modifier: Modifier,
    categories: List<String>,
    selectedCategory: String,
    onCategory: (String) -> Unit,
    search: String,
    onSearch: (String) -> Unit,
    products: List<ProductEntity>,
    onProduct: (ProductEntity) -> Unit,
    openAmountEnabled: Boolean = false,
    onOpenAmount: () -> Unit = {},
    openAmountCategories: List<String> = emptyList(),
    onOpenAmountCategory: (String) -> Unit = {},
    onOpenSections: () -> Unit = {},
    hideBarcoded: Boolean = false,
    onToggleHideBarcoded: (() -> Unit)? = null,
    uppercaseNames: Boolean = false,
) {
    val categoryListState = rememberLazyListState()
    val selectedIndex = remember(categories, selectedCategory) {
        categories.indexOf(selectedCategory)
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            categoryListState.animateScrollToItem(maxOf(0, selectedIndex - 1))
        }
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearch,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escanea un producto", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    readOnly = true,
                    colors = catalogFieldColors(),
                )
                if (onToggleHideBarcoded != null) {
                    Spacer(Modifier.width(6.dp))
                    PosButton(
                        label = if (hideBarcoded) "Sin código de barras" else "Todos",
                        click = onToggleHideBarcoded,
                        selected = hideBarcoded,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PosButton(
                    label = "☰",
                    click = onOpenSections,
                    modifier = Modifier.padding(end = 6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                )
                LazyRow(
                    state = categoryListState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(categories, key = { it }) { item ->
                        PosButton(
                            label = item,
                            click = { onCategory(item) },
                            selected = selectedCategory == item,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(6.dp))

            val openCategoriesFiltered = remember(openAmountCategories, selectedCategory, search) {
                openAmountCategories.filter { cat ->
                    val matchesCategory = selectedCategory == "Todos" || selectedCategory == "Monto Abierto" || cat.contains(selectedCategory, ignoreCase = true) || selectedCategory.contains(cat, ignoreCase = true)
                    val matchesSearch = search.isBlank() || cat.contains(search, ignoreCase = true)
                    matchesCategory && matchesSearch
                }
            }

            if ((products.isEmpty() && selectedCategory != "Monto Abierto") || (selectedCategory == "Monto Abierto" && openCategoriesFiltered.isEmpty())) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("No hay productos para esta búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (openAmountEnabled && search.isNotBlank()) {
                            PosButton("¿Agregar como Producto Abierto?", onOpenAmount, primary = true)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selectedCategory == "Monto Abierto") {
                        items(openCategoriesFiltered) { cat ->
                            OpenProductTile(cat, uppercaseNames) { onOpenAmountCategory(cat) }
                        }
                    } else {
                        items(products, key = { it.id }) { ProductTile(it, uppercaseNames) { onProduct(it) } }
                    }
                }
            }
        }
    }
}


// Display-only casing for catalog tiles; the stored catalog name is never modified.
internal fun tileName(name: String, uppercase: Boolean): String =
    if (uppercase) name.uppercase(java.util.Locale.ROOT) else name

// Shows a virtual product tile for open amount categories.
@Composable
internal fun OpenProductTile(category: String, uppercaseNames: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(tileName("$category (Abierto)", uppercaseNames), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column {
                Text("Capturar precio", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// Shows product price in one compact, high-target catalog tile.
@Composable
internal fun ProductTile(product: ProductEntity, uppercaseNames: Boolean = false, onClick: () -> Unit) {
    val unavailable = !product.available

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        enabled = !unavailable,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(tileName(product.name, uppercaseNames), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column {
                Text(Money.format(Money.fromCatalog(product.price)), fontWeight = FontWeight.Bold)
                if (unavailable) {
                    Text("No disponible", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
                product.sku.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { sku ->
                    Text(
                        text = sku,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

