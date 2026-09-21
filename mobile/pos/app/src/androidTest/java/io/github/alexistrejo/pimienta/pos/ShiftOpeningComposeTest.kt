package io.github.alexistrejo.pimienta.pos

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.seed.TrainingBootstrapImporter
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented UI tests verifying the multi-step shift opening workflow and role restrictions.
@RunWith(AndroidJUnit4::class)
class ShiftOpeningComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var provider: PosDatabaseProvider
    private lateinit var repository: PosRepository

    // Standard PIN hash for PIN "1234" in training mode
    private val pinHash1234 = "3383b6e47c9df8a2ff5f39fc976ddf7ae2591fa84434bb58594eef7a45981354"

    private val mgr1 = LocalUserEntity("user-mgr-1", "Gerente Carlos", "MANAGER", pinHash1234, true)
    private val mgr2 = LocalUserEntity("user-mgr-2", "Gerente Ana", "MANAGER", pinHash1234, true)
    private val admin1 = LocalUserEntity("user-admin-1", "Administrador Luis", "ADMIN", pinHash1234, true)
    private val cashier1 = LocalUserEntity("user-cashier-1", "Cajero Pedro", "CASHIER", pinHash1234, true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = PosDatabaseProvider(context)
        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))
        repository = PosRepository(provider, RuntimeMode.SANDBOX)
    }

    @After
    fun tearDown() {
        provider.close()
        context.deleteDatabase(PosDatabaseProvider.TRAINING_DB_NAME)
    }

    @Test
    fun step1DisplaysOnlyManagersAndAdmins() {
        val users = listOf(mgr1, admin1, cashier1)

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                Access(
                    users = users,
                    repository = repository,
                    notice = null,
                    mode = RuntimeMode.SANDBOX,
                    opened = {},
                    message = {},
                )
            }
        }

        // Authorizer selection must show Manager and Admin but NOT Cashier
        composeRule.onNodeWithText(mgr1.displayTitle(true)).assertIsDisplayed()
        composeRule.onNodeWithText(admin1.displayTitle(true)).assertIsDisplayed()
        composeRule.onAllNodesWithText(cashier1.displayTitle(true)).assertCountEquals(0)
    }

    @Test
    fun step1InvalidPinShowsError() {
        val users = listOf(mgr1, cashier1)

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                Access(
                    users = users,
                    repository = repository,
                    notice = null,
                    mode = RuntimeMode.SANDBOX,
                    opened = {},
                    message = {},
                )
            }
        }

        // Enter wrong PIN "9999"
        repeat(4) {
            composeRule.onAllNodesWithText("9")[0].performClick()
        }
        composeRule.onNodeWithText("Continuar a asignar cajero y fondo →").performClick()

        // Verify error message is shown
        composeRule.onNodeWithText("El PIN no corresponde al perfil seleccionado.").assertIsDisplayed()
    }

    @Test
    fun step1ValidPinMovesToStep2AndFiltersAssigneesForManager() {
        val users = listOf(mgr1, mgr2, cashier1)

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                Access(
                    users = users,
                    repository = repository,
                    notice = null,
                    mode = RuntimeMode.SANDBOX,
                    opened = {},
                    message = {},
                )
            }
        }

        // Enter correct PIN "1234" for Gerente Carlos
        composeRule.onAllNodesWithText("1")[0].performClick()
        composeRule.onAllNodesWithText("2")[0].performClick()
        composeRule.onAllNodesWithText("3")[0].performClick()
        composeRule.onAllNodesWithText("4")[0].performClick()
        composeRule.onNodeWithText("Continuar a asignar cajero y fondo →").performClick()

        // Verify Step 2 is shown
        composeRule.onNodeWithText("Paso 2: Asignación de cajero y fondo inicial").assertIsDisplayed()
        composeRule.onNodeWithText("Autorizado por: ${mgr1.displayName} (${mgr1.spanishRoleLabel})").assertIsDisplayed()

        // Gerente Carlos can select himself and Cajero Pedro, but NOT Gerente Ana
        composeRule.onNodeWithText(mgr1.displayTitle(true)).assertIsDisplayed()
        composeRule.onNodeWithText(cashier1.displayTitle(true)).assertIsDisplayed()
        composeRule.onAllNodesWithText(mgr2.displayTitle(true)).assertCountEquals(0)
    }

    @Test
    fun step2BackButtonReturnsToStep1() {
        val users = listOf(mgr1, cashier1)

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                Access(
                    users = users,
                    repository = repository,
                    notice = null,
                    mode = RuntimeMode.SANDBOX,
                    opened = {},
                    message = {},
                )
            }
        }

        // Enter correct PIN "1234"
        composeRule.onAllNodesWithText("1")[0].performClick()
        composeRule.onAllNodesWithText("2")[0].performClick()
        composeRule.onAllNodesWithText("3")[0].performClick()
        composeRule.onAllNodesWithText("4")[0].performClick()
        composeRule.onNodeWithText("Continuar a asignar cajero y fondo →").performClick()

        // Click Cambiar autorizador button
        composeRule.onNodeWithText("← Cambiar autorizador").performClick()

        // Verify Step 1 is visible again
        composeRule.onNodeWithText("Paso 1: Identificación del autorizador").assertIsDisplayed()
    }

    @Test
    fun step2OpeningShiftInvokesOpenedCallbackWithSelectedAssignee() {
        var openedShift: ShiftEntity? = null
        val users = listOf(mgr1, cashier1)

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                Access(
                    users = users,
                    repository = repository,
                    notice = null,
                    mode = RuntimeMode.SANDBOX,
                    opened = { shift -> openedShift = shift },
                    message = {},
                )
            }
        }

        // Step 1: Authenticate Gerente Carlos with PIN "1234"
        composeRule.onAllNodesWithText("1")[0].performClick()
        composeRule.onAllNodesWithText("2")[0].performClick()
        composeRule.onAllNodesWithText("3")[0].performClick()
        composeRule.onAllNodesWithText("4")[0].performClick()
        composeRule.onNodeWithText("Continuar a asignar cajero y fondo →").performClick()

        // Step 2: Select Cajero Pedro as assignee
        composeRule.onNodeWithText(cashier1.displayTitle(true)).performClick()

        // Enter opening cash: 500
        composeRule.onAllNodesWithText("5")[0].performClick()
        composeRule.onAllNodesWithText("0")[0].performClick()
        composeRule.onAllNodesWithText("0")[0].performClick()

        // Click Confirmar y abrir turno
        composeRule.onNodeWithText("Confirmar y abrir turno").performClick()
        composeRule.waitForIdle()

        assertNotNull(openedShift)
        assertEquals(cashier1.id, openedShift?.cashierId)
        assertEquals(500L, openedShift?.openingCashCentavos)
    }
}
