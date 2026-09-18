package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingCatalogDialogComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionAddsThisSaleOnly() {
        var added = 0L
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                PendingCatalogDialog(
                    barcode = "750111",
                    onDismiss = {},
                    onConfirm = { added = it },
                    onSaveToCatalog = {},
                )
            }
        }
        composeRule.onNodeWithText("Código no está en el catálogo").assertIsDisplayed()
        composeRule.onAllNodesWithText("4")[0].performClick()
        repeat(4) { composeRule.onAllNodesWithText("4")[1].performClick() }
        composeRule.onNodeWithText("Agregar a esta venta").performClick()
        assertEquals(400L, added)
    }

    @Test
    fun secondaryActionOpensCatalogSave() {
        var save = false
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                PendingCatalogDialog(
                    barcode = "750111",
                    onDismiss = {},
                    onConfirm = {},
                    onSaveToCatalog = { save = true },
                )
            }
        }
        composeRule.onNodeWithText("Guardar en catálogo").performClick()
        assertTrue(save)
    }
}
