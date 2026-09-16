package io.github.alexistrejo.pimienta.pos.data.local
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CatalogCategoryEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity
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
}
