package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Verifies that the catalog sections dialog renders categories in a grid and handles selection.
@RunWith(AndroidJUnit4::class)
class SectionsDialogComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysCategoriesAndSelectsOption() {
        var selectedCategory: String? = null
        var dismissed = false
        val categories = listOf("Todos", "Deli", "Bebidas preparadas", "Dulces", "Botanas", "Japonesa")

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                SectionsDialog(
                    categories = categories,
                    selected = "Todos",
                    onDismiss = { dismissed = true },
                    onSelect = { selectedCategory = it }
                )
            }
        }

        // Verify title and categories are rendered
        composeRule.onNodeWithText("Secciones del catálogo").assertIsDisplayed()
        composeRule.onNodeWithText("Bebidas preparadas").assertIsDisplayed()

        // Select a category and verify callbacks trigger
        composeRule.onNodeWithText("Bebidas preparadas").performClick()
        assertEquals("Bebidas preparadas", selectedCategory)
        assertTrue(dismissed)
    }

    @Test
    fun dismissesOnCloseClick() {
        var dismissed = false
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                SectionsDialog(
                    categories = listOf("Todos", "Deli"),
                    selected = "Todos",
                    onDismiss = { dismissed = true },
                    onSelect = {}
                )
            }
        }

        composeRule.onNodeWithText("Cerrar").performClick()
        assertTrue(dismissed)
    }
}
