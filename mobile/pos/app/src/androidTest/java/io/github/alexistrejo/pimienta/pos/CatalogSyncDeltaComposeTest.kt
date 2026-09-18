package io.github.alexistrejo.pimienta.pos

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import io.github.alexistrejo.pimienta.pos.data.sync.ChangeOp
import io.github.alexistrejo.pimienta.pos.data.sync.ChangesResponse
import io.github.alexistrejo.pimienta.pos.data.sync.ProductDto
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.ui.theme.PosTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Verifies that incremental catalog delta syncs update the database and recompose the UI live.
@RunWith(AndroidJUnit4::class)
class CatalogSyncDeltaComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var provider: PosDatabaseProvider
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        provider = PosDatabaseProvider(context)
        provider.modes.setMode(RuntimeMode.PRODUCTION)
        val db = provider.database(RuntimeMode.PRODUCTION)

        db.runInTransaction {
            db.productDao().clear()
            db.siteDao().clear()
            db.siteDao().insert(SiteEntity("site-001", "Sede Central", "Av. Principal", "MXN"))
        }
    }

    @After
    fun tearDown() {
        val db = provider.database(RuntimeMode.PRODUCTION)
        db.runInTransaction {
            db.productDao().clear()
            db.siteDao().clear()
        }
    }

    // Verifies product creation and subsequent name/price updates reflect in Room and Compose.
    @Test
    fun syncAddsProductAndThenUpdatesNameAndPriceLiveInUi() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = provider.database(RuntimeMode.PRODUCTION)
        val provisioning = ProvisioningRepository(context, provider)
        var liveProducts by mutableStateOf<List<ProductEntity>>(emptyList())

        // Initial Compose setup observing catalog state.
        composeRule.setContent {
            PosTheme(darkTheme = true) {
                CatalogPanel(
                    modifier = androidx.compose.ui.Modifier,
                    categories = listOf("Todos", "Bebidas"),
                    selectedCategory = "Todos",
                    onCategory = {},
                    search = "",
                    onSearch = {},
                    products = liveProducts,
                    onProduct = {},
                )
            }
        }

        // 1. First sync: Add new product "Jugo de Naranja" at $25.00
        val productV1 = ProductDto(
            id = "prod-001",
            sku = "SKU-JUGO",
            barcode = "750100011",
            name = "Jugo de Naranja",
            saleCategory = "Bebidas",
            unit = "PZA",
            priceCentavos = 2500,
            costCentavos = 1000,
            available = true,
            stockQuantity = 50,
            stockMinQuantity = 5,
            stockPolicy = "UNLIMITED",
        )
        val payloadV1 = json.encodeToJsonElement(ProductDto.serializer(), productV1)
        val changes1 = ChangesResponse(
            schemaVersion = 1,
            nextCursor = "cursor-v1",
            operations = listOf(ChangeOp("upsert", "product", "prod-001", payloadV1)),
        )

        // Apply first sync delta.
        provisioning.applyChanges(changes1)

        // Update Compose state with current Room emission.
        liveProducts = db.productDao().getAll()
        composeRule.waitForIdle()

        // Assert first sync is displayed in UI.
        composeRule.onNodeWithText("Jugo de Naranja").assertIsDisplayed()
        composeRule.onNodeWithText("$25.00").assertIsDisplayed()
        assertEquals(1, liveProducts.size)
        assertEquals("Jugo de Naranja", liveProducts.single().name)
        assertEquals("25.00", liveProducts.single().price)

        // 2. Second sync: Update same product to "Jugo de Naranja Natural 500ml" at $32.00
        val productV2 = productV1.copy(
            name = "Jugo de Naranja Natural 500ml",
            priceCentavos = 3200,
        )
        val payloadV2 = json.encodeToJsonElement(ProductDto.serializer(), productV2)
        val changes2 = ChangesResponse(
            schemaVersion = 1,
            nextCursor = "cursor-v2",
            operations = listOf(ChangeOp("upsert", "product", "prod-001", payloadV2)),
        )

        // Apply second sync delta.
        provisioning.applyChanges(changes2)

        // Update Compose state with updated Room emission.
        liveProducts = db.productDao().getAll()
        composeRule.waitForIdle()

        // Assert second sync (updated name & price) is displayed live in UI.
        composeRule.onNodeWithText("Jugo de Naranja Natural 500ml").assertIsDisplayed()
        composeRule.onNodeWithText("$32.00").assertIsDisplayed()
        assertEquals(1, liveProducts.size)
        assertEquals("Jugo de Naranja Natural 500ml", liveProducts.single().name)
        assertEquals("32.00", liveProducts.single().price)
    }
}
