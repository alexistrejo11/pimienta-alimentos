package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
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

    private val manager = LocalUserEntity("42", "Manager", "MANAGER", "hash", true)

    @Test
    fun catalogActionIsVisibleOnlyWhenPolicyAllowsIt() {
        composeRule.setContent {
            PosTheme(darkTheme = true) { CatalogPanel(androidx.compose.ui.Modifier, listOf("Todos"), "Todos", {}, "", {}, emptyList(), {}, false) }
        }
        composeRule.onAllNodesWithText("Monto abierto").assertCountEquals(0)

        composeRule.setContent {
            PosTheme(darkTheme = true) { CatalogPanel(androidx.compose.ui.Modifier, listOf("Todos"), "Todos", {}, "", {}, emptyList(), {}, true) }
        }
        composeRule.onNodeWithText("Monto abierto").assertIsDisplayed()
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
                OpenAmountDialog(listOf("Bebidas"), listOf(manager), { _, _ -> true }, {}, { _, _, _, _ -> })
            }
        }
        composeRule.onNodeWithText("Producto abierto · Bebidas").assertIsDisplayed()
        composeRule.onNodeWithText("Autorizar y agregar").performClick()
        composeRule.onNodeWithText("Captura un importe válido.").assertIsDisplayed()
    }

    @Test
    fun failedPinKeepsSurfaceOpenAndShowsError() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(listOf("Bebidas"), listOf(manager), { _, _ -> false }, {}, { _, _, _, _ -> })
            }
        }
        composeRule.onAllNodesWithText("4")[0].performClick()
        repeat(4) { composeRule.onAllNodesWithText("4")[1].performClick() }
        composeRule.onNodeWithText("Autorizar y agregar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PIN inválido o autorizador inactivo.").assertIsDisplayed()
    }

    @Test
    fun validPinCreatesOneAuthorizedLine() {
        var created = false
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(
                    listOf("Bebidas"), listOf(manager), { _, _ -> true },
                    {},
                ) { category, amount, authorizer, _ ->
                    created = category == "Bebidas" && amount == 400L && authorizer.id == "42"
                }
            }
        }
        composeRule.onAllNodesWithText("4")[0].performClick()
        repeat(4) { composeRule.onAllNodesWithText("4")[1].performClick() }
        composeRule.onNodeWithText("Autorizar y agregar").performClick()
        composeRule.waitForIdle()
        assertTrue(created)
    }

    @Test
    fun cancellationDoesNotCreateLine() {
        var created = false
        var cancelled = false
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                OpenAmountDialog(listOf("Bebidas"), listOf(manager), { _, _ -> true }, { cancelled = true }) { _, _, _, _ -> created = true }
            }
        }
        composeRule.onNodeWithText("Cancelar").performClick()
        assertTrue(cancelled)
        assertEquals(false, created)
    }
}
