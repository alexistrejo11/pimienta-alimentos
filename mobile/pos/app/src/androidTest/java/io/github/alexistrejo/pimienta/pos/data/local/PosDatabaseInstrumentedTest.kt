package io.github.alexistrejo.pimienta.pos.data.local
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CatalogCategoryEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import io.github.alexistrejo.pimienta.pos.data.telemetry.PosTelemetryLogger
import org.junit.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class PosDatabaseInstrumentedTest {
 private lateinit var db: PosDatabase
 @Before fun setUp(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),PosDatabase::class.java).allowMainThreadQueries().build()}
 @After fun tearDown(){db.close()}
 @Test fun syncStatePersistsCursor(){db.syncDao().saveState(SyncStateEntity(baseUrl="https://backend/",changesCursor="c1",status="ONLINE"));Assert.assertEquals("c1",db.syncDao().state()?.changesCursor)}

 // Verifies the Phase 3 projections and cursor commit together in one Room transaction.
 @Test fun phase3ProjectionStoresPoliciesCategoriesAndSequenceCursorAtomically(){
  db.runInTransaction {
   db.syncProjectionDao().insertPolicy(PosPolicyEntity(siteId="1",allowNegativeStock=true,allowOpenProducts=true,defaultNegativeStockLimit=5,staleCatalogWarnHours=24,staleCatalogBlockHours=72,openAmountCategoriesJson="[\"Bebidas\"]"))
   db.syncProjectionDao().insertCategories(listOf(CatalogCategoryEntity("1","Bebidas")))
   db.syncDao().saveState(SyncStateEntity(baseUrl="https://backend/",changesCursor="cursor-hq-1-s42",status="ONLINE"))
  }
  Assert.assertTrue(db.syncProjectionDao().policy()!!.allowOpenProducts)
  Assert.assertEquals(listOf("Bebidas"),db.syncProjectionDao().activeCategories("1").map { it.name })
  Assert.assertEquals("cursor-hq-1-s42",db.syncDao().state()?.changesCursor)
 }

  @Test fun telemetryQueueIsBoundedAndReturnsBatches(){
  val logger=PosTelemetryLogger(db)
  repeat(205){logger.record("debug","test-event","event-$it")}
  Assert.assertEquals(200,db.operationsDao().telemetryCount())
  Assert.assertEquals(50,db.operationsDao().pendingTelemetry(50).size)
  }

  // Verifies that authorization evidence survives Room persistence for one open line.
  @Test fun openAmountLineStoresAuthorizationEvidenceWithoutInventoryRows(){
   db.operationsDao().insertLines(listOf(SaleLineEntity("line-1", "sale-1", null, "Producto abierto · Bebidas", "Bebidas", 1, 4500, 4500, "NOT_CONTROLLED", "OPEN_AMOUNT", null, 42, 1726358400000)))
   val line = db.operationsDao().linesForSale("sale-1").single()
   Assert.assertEquals(42L, line.authorizedByOperatorId)
   Assert.assertEquals(1726358400000L, line.authorizedAtEpochMillis)
   Assert.assertTrue(db.operationsDao().movementsBetween(0, Long.MAX_VALUE).isEmpty())
  }

  // Verifies that catalog changes emit immediately to the Compose-facing Room flow.
  @Test fun productFlowEmitsRemoteCatalogChanges() = runBlocking {
   Assert.assertTrue(db.productDao().observeAll().first().isEmpty())
   db.productDao().insertAll(listOf(ProductEntity("p1", null, "SKU-1", null, null, "Agua", "Bebidas", "PIECE", "10.00", "5.00", true, "10", "0", "UNLIMITED", null, null)))
   Assert.assertEquals("Agua", db.productDao().observeAll().first().single().name)
  }

  // Verifies that unsynced local deductions remain visible over a newer central snapshot.
  @Test fun pendingStockDeltaIsKeptSeparateFromCentralSnapshot(){
   db.productDao().insertAll(listOf(ProductEntity("p1", null, "SKU-1", null, null, "Agua", "Bebidas", "PIECE", "10.00", "5.00", true, "10", "0", "CONTROLLED", null, null, "10")))
   db.operationsDao().insertOutbox(OutboxEventEntity("event-1", 1, "SALE_CONFIRMED", "sale-1", "PENDING", 1))
   db.operationsDao().insertMovements(listOf(InventoryMovementEntity("movement-1", "sale-1", "p1", -2, 1, syncEventId="event-1")))
   Assert.assertEquals(8, db.productDao().getAll().single().stock.toBigDecimal().toInt())
   Assert.assertEquals("10", db.productDao().getAll().single().centralStock)
   db.syncDao().markRejected("event-1", "invalid device")
   Assert.assertEquals(10, db.productDao().getAll().single().stock.toBigDecimal().toInt())
  }

  // Verifies that a pending cancellation reverses only its own sale movement.
  @Test fun cancellationUsesItsOwnOutboxEvent(){
   db.productDao().insertAll(listOf(ProductEntity("p1", null, "SKU-1", null, null, "Agua", "Bebidas", "PIECE", "10.00", "5.00", true, "8", "0", "CONTROLLED", null, null, "8")))
   db.operationsDao().insertOutbox(OutboxEventEntity("sale-event", 1, "SALE_CONFIRMED", "sale-1", "SYNCED", 1))
   db.operationsDao().insertOutbox(OutboxEventEntity("cancel-event", 2, "SALE_CANCELLED", "sale-1", "PENDING", 2))
   db.operationsDao().insertMovements(listOf(
    InventoryMovementEntity("sale-movement", "sale-1", "p1", -2, 1, syncEventId="sale-event"),
    InventoryMovementEntity("cancel-movement", "sale-1", "p1", 2, 2, "SALE_CANCELLATION", "cancel-event"),
   ))
   Assert.assertEquals(10, db.productDao().getAll().single().stock.toBigDecimal().toInt())
  }

  // Verifies that operational movements do not alter products whose stock is not controlled.
  @Test fun pendingMovementDoesNotAlterUnlimitedStock(){
   db.productDao().insertAll(listOf(ProductEntity("p1", null, "SKU-1", null, null, "Agua", "Bebidas", "PIECE", "10.00", "5.00", true, "10", "0", "UNLIMITED", null, null, "10")))
   db.operationsDao().insertOutbox(OutboxEventEntity("event-1", 1, "RESTOCK_RECORDED", "movement-1", "PENDING", 1))
   db.operationsDao().insertMovements(listOf(InventoryMovementEntity("movement-1", "movement-1", "p1", 5, 1, "RESTOCK", "event-1")))
   Assert.assertEquals(10, db.productDao().getAll().single().stock.toBigDecimal().toInt())
  }
 }
