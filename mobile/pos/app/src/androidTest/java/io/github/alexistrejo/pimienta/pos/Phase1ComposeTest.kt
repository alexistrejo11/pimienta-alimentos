package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                    syncLabel = "Inventario sincronizado hace 0 min",
                    dark = true,
                    onTheme = {},
                    landscape = false,
                    openManager = {},
                    openDevices = {},
                    openWithdrawal = {},
                    withdrawalEnabled = true,
                    printerLabel = "Impresora lista",
                    printerAlert = false,
                    scannerLabel = "Lector HID",
                )
            }
        }

        composeRule.onNodeWithText("POS").assertIsDisplayed()
    }

    @Test
    fun statusBarActionsExposeDevicesWithoutManagerPin() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                StatusBar(
                    cashier = "Cajera",
                    pending = 0,
                    syncLabel = "Inventario sincronizado hace 0 min",
                    dark = true,
                    onTheme = {},
                    landscape = false,
                    openManager = {},
                    openDevices = {},
                    openWithdrawal = {},
                    withdrawalEnabled = true,
                    printerLabel = "Impresora lista",
                    printerAlert = false,
                    scannerLabel = "Lector HID",
                )
            }
        }

        composeRule.onNodeWithText("Acciones ▼").performClick()
        composeRule.onNodeWithText("Dispositivos").assertIsDisplayed()
        composeRule.onNodeWithText("Panel Manager").assertIsDisplayed()
    }

    @Test
    fun devicePresenceChipShowsBrandLabel() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                DevicePresenceChip("Impresora USB")
            }
        }
        composeRule.onNodeWithText("Impresora USB").assertIsDisplayed()
    }
}
