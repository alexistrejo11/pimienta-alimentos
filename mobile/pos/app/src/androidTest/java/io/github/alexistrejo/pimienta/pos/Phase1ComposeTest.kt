package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import org.junit.Rule
import org.junit.Test

// Verifies the operational folio indicator remains visible in the register header.
class Phase1ComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusBarShowsNextFolioBeforeCheckout() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                StatusBar(
                    cashier = "Cajera",
                    pending = 0,
                    dark = true,
                    onTheme = {},
                    landscape = false,
                    lockCashRegister = {},
                    openManager = {},
                    openWithdrawal = {},
                    withdrawalEnabled = true,
                    printerLabel = "Impresora lista",
                    printerAlert = false,
                    scannerLabel = "Lector HID",
                    nextFolio = "T1-ABCD-0001",
                )
            }
        }

        composeRule.onNodeWithText("Próximo folio · T1-ABCD-0001").assertIsDisplayed()
    }
}
