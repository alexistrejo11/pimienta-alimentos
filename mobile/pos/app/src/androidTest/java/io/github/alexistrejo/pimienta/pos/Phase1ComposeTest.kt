package io.github.alexistrejo.pimienta.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.alexistrejo.pimienta.pos.hardware.DeviceFix
import io.github.alexistrejo.pimienta.pos.hardware.DeviceStatusLine
import io.github.alexistrejo.pimienta.pos.hardware.DeviceTone
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
    fun deviceStatusRowShowsStateAndFix() {
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                DeviceStatusRow(
                    device = "Impresora",
                    line = DeviceStatusLine("Cable conectado · falta permiso", "Toca Dar permiso USB.", DeviceTone.BLOCKED, DeviceFix.GRANT_USB),
                    fixLabel = "Dar permiso USB",
                )
            }
        }
        composeRule.onNodeWithText("Cable conectado · falta permiso").assertIsDisplayed()
        composeRule.onNodeWithText("Dar permiso USB").assertIsDisplayed()
    }
}
