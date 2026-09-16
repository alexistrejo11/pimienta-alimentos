package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import org.junit.Rule
import org.junit.Test

    // Verifies the operational register header remains visible before checkout.
class Phase1ComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusBarShowsRegisterHeaderBeforeCheckout() {
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
                )
            }
        }

        composeRule.onNodeWithText("Punto de Venta").assertIsDisplayed()
    }
}
