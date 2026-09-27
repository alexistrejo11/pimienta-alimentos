package io.github.alexistrejo.pimienta.pos

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented Compose UI tests verifying the Corte de Caja (Z-Close) workflow and smooth shift closure across orientation changes.
@RunWith(AndroidJUnit4::class)
class ShiftCloseComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var provider: PosDatabaseProvider
    private lateinit var repository: PosRepository
    private lateinit var activeShift: ShiftEntity

    private val pinHash1234 = "3383b6e47c9df8a2ff5f39fc976ddf7ae2591fa84434bb58594eef7a45981354"
    private val mgr1 = LocalUserEntity("user-mgr-1", "Gerente Carlos", "MANAGER", pinHash1234, true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = PosDatabaseProvider(context)
        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))
        repository = PosRepository(provider, RuntimeMode.SANDBOX)

        // Open an active shift for testing
        activeShift = repository.openShift(mgr1.id, 50_000L)!!
    }

    @After
    fun tearDown() {
        provider.close()
        context.deleteDatabase(PosDatabaseProvider.TRAINING_DB_NAME)
    }

    @Test
    fun zCloseFlowCompletesAndInvokesShiftClosedCallback() {
        var shiftClosedCalled = false

        composeRule.setContent {
            PosTheme(darkTheme = true) {
                ManagerPanel(
                    shift = activeShift,
                    manager = mgr1,
                    users = listOf(mgr1),
                    products = emptyList(),
                    pendingEvents = 0,
                    repository = repository,
                    onReturnToSale = {},
                    onShiftClosed = { shiftClosedCalled = true },
                )
            }
        }

        // Navigate to "Caja y Corte de Caja" section
        composeRule.onNodeWithText("Sección: Resumen del día").performClick()
        composeRule.onNodeWithText("Caja y Corte de Caja").performClick()

        // Click "Iniciar Corte de Caja"
        composeRule.onNodeWithText("Iniciar Corte de Caja").performClick()

        // Verify "Conteo ciego de caja" dialog is shown
        composeRule.onNodeWithText("Conteo ciego de caja").assertIsDisplayed()

        // Enter count: 500
        composeRule.onAllNodesWithText("5")[0].performClick()
        composeRule.onAllNodesWithText("0")[0].performClick()
        composeRule.onAllNodesWithText("0")[0].performClick()

        // Click "Enviar a validación"
        composeRule.onNodeWithText("Enviar a validación").performClick()

        // Verify Validation stage is shown
        composeRule.onNodeWithText("Validar Corte de Caja · ${mgr1.displayName}").assertIsDisplayed()

        // Click "Aprobar con PIN"
        composeRule.onNodeWithText("Aprobar con PIN").performClick()

        // Enter PIN: "1234"
        composeRule.onAllNodesWithText("1")[0].performClick()
        composeRule.onAllNodesWithText("2")[0].performClick()
        composeRule.onAllNodesWithText("3")[0].performClick()
        composeRule.onAllNodesWithText("4")[0].performClick()

        // Wait for asynchronous shift close transaction to complete
        composeRule.waitForIdle()

        // Verify completion screen "✓ Corte de Caja Completado" is displayed
        composeRule.onNodeWithText("✓ Corte de Caja Completado").assertIsDisplayed()

        // Click "Finalizar y salir al inicio" button
        composeRule.onNodeWithText("Finalizar y salir al inicio", substring = true).performClick()
        composeRule.waitForIdle()

        assertTrue(shiftClosedCalled)
        // Verify shift status in database is CLOSED
        assertEquals("CLOSED", repository.findShift(activeShift.id)?.status)
    }

    @Test
    fun zCloseFlowSurvivesScreenRotationAcrossAllStages() {
        var shiftClosedCalled = false
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            PosTheme(darkTheme = true) {
                ManagerPanel(
                    shift = activeShift,
                    manager = mgr1,
                    users = listOf(mgr1),
                    products = emptyList(),
                    pendingEvents = 0,
                    repository = repository,
                    onReturnToSale = {},
                    onShiftClosed = { shiftClosedCalled = true },
                )
            }
        }

        // Navigate to "Caja y Corte de Caja" section
        composeRule.onNodeWithText("Sección: Resumen del día").performClick()
        composeRule.onNodeWithText("Caja y Corte de Caja").performClick()
        composeRule.onNodeWithText("Iniciar Corte de Caja").performClick()

        // 1. Enter count in COUNTING stage
        composeRule.onAllNodesWithText("5")[0].performClick()
        composeRule.onAllNodesWithText("0")[0].performClick()
        composeRule.onAllNodesWithText("0")[0].performClick()

        // Simulate screen rotation during COUNTING stage
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify blind count dialog is preserved after rotation
        composeRule.onNodeWithText("Conteo ciego de caja").assertIsDisplayed()

        // 2. Submit to VALIDATION stage
        composeRule.onNodeWithText("Enviar a validación").performClick()

        // Simulate screen rotation during VALIDATION stage
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify validation dialog is preserved after rotation
        composeRule.onNodeWithText("Validar Corte de Caja · ${mgr1.displayName}").assertIsDisplayed()

        // 3. Approve with PIN "1234" to reach COMPLETED stage
        composeRule.onNodeWithText("Aprobar con PIN").performClick()
        composeRule.onAllNodesWithText("1")[0].performClick()
        composeRule.onAllNodesWithText("2")[0].performClick()
        composeRule.onAllNodesWithText("3")[0].performClick()
        composeRule.onAllNodesWithText("4")[0].performClick()
        composeRule.waitForIdle()

        // Simulate screen rotation during COMPLETED stage (shift is now CLOSED in DB)
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify completion screen "✓ Corte de Caja Completado" is STILL displayed after rotation
        composeRule.onNodeWithText("✓ Corte de Caja Completado").assertIsDisplayed()

        // Click "Finalizar y salir al inicio" button
        composeRule.onNodeWithText("Finalizar y salir al inicio", substring = true).performClick()
        composeRule.waitForIdle()

        assertTrue(shiftClosedCalled)
        assertEquals("CLOSED", repository.findShift(activeShift.id)?.status)
    }
}
