package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.alexistrejo.pimienta.pos.domain.formatOpenAmountProductName
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Covers the cashier-facing Open Product surface without mixing barcode exceptions.
@RunWith(AndroidJUnit4::class)
class Phase5OpenAmountComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalogActionIsHiddenWhenPolicyDisallowsIt() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                CatalogPanel(androidx.compose.ui.Modifier, listOf("Todos"), "Todos", {}, "cafe", {}, emptyList(), {}, false)
            }
        }
        composeRule.onAllNodesWithText("¿Agregar como Producto Abierto?").assertCountEquals(0)
    }

    @Test
    fun catalogActionIsVisibleWhenPolicyAllowsIt() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                CatalogPanel(androidx.compose.ui.Modifier, listOf("Todos"), "Todos", {}, "cafe", {}, emptyList(), {}, true)
            }
        }
        composeRule.onNodeWithText("¿Agregar como Producto Abierto?").assertIsDisplayed()
    }

    @Test
    fun emptySearchOffersExplicitOpenProductAction() {
        composeRule.setContent {
            PosTheme(darkTheme = true) { CatalogPanel(androidx.compose.ui.Modifier, listOf("Todos"), "Todos", {}, "cafe", {}, emptyList(), {}, true) }
        }
        composeRule.onNodeWithText("¿Agregar como Producto Abierto?").assertIsDisplayed()
    }

    @Test
    fun dialogShowsGeneratedDescriptionAndValidatesAmount() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(categories = listOf("Bebidas"), onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        composeRule.onNodeWithText("Monto Abierto · Bebidas").assertIsDisplayed()
        composeRule.onNodeWithText("Agregar al carrito").performClick()
        composeRule.onNodeWithText("Captura un importe válido.").assertIsDisplayed()
    }

    @Test
    fun validAmountCreatesOneLine() {
        var payload: Triple<String, Long, String>? = null
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(
                    categories = listOf("Bebidas"),
                    onDismiss = {},
                    onConfirm = { category, amount, concept ->
                        payload = Triple(category, amount, concept)
                    },
                )
            }
        }
        composeRule.onAllNodesWithText("4")[0].performClick()
        repeat(2) { composeRule.onAllNodesWithText("0")[0].performClick() }
        composeRule.onNodeWithText("Agregar al carrito").performClick()
        composeRule.waitForIdle()
        assertEquals(Triple("Bebidas", 40_000L, ""), payload)
    }

    @Test
    fun cancellationDoesNotCreateLine() {
        var created = false
        var cancelled = false
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(
                    categories = listOf("Bebidas"),
                    onDismiss = { cancelled = true },
                    onConfirm = { _, _, _ -> created = true },
                )
            }
        }
        composeRule.onNodeWithText("Cancelar").performClick()
        assertTrue(cancelled)
        assertEquals(false, created)
    }

    @Test
    fun conceptUpdatesPreviewAndConfirmPayload() {
        var payload: Triple<String, Long, String>? = null
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(
                    categories = listOf("Bebidas"),
                    onDismiss = {},
                    onConfirm = { category, amount, concept ->
                        payload = Triple(category, amount, concept)
                    },
                )
            }
        }
        composeRule.onNodeWithText("Monto Abierto · Bebidas").assertIsDisplayed()
        composeRule.onNodeWithText("Concepto (opcional)").performTextInput("5 cucharas")
        composeRule.onNodeWithText("Monto Abierto · Bebidas (5 cucharas)").assertIsDisplayed()
        composeRule.onAllNodesWithText("4")[0].performClick()
        repeat(2) { composeRule.onAllNodesWithText("0")[0].performClick() }
        composeRule.onNodeWithText("Agregar al carrito").performClick()
        composeRule.waitForIdle()
        assertEquals(Triple("Bebidas", 40_000L, "5 cucharas"), payload)
        assertEquals("Monto Abierto · Bebidas (5 cucharas)", formatOpenAmountProductName(payload!!.first, payload.third))
    }
}
