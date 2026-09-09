package io.github.alexistrejo.pimienta.pos.data.local
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import org.junit.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class PosDatabaseInstrumentedTest {
 private lateinit var db: PosDatabase
 @Before fun setUp(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),PosDatabase::class.java).allowMainThreadQueries().build()}
 @After fun tearDown(){db.close()}
 @Test fun syncStatePersistsCursor(){db.syncDao().saveState(SyncStateEntity(baseUrl="https://backend/",changesCursor="c1",status="ONLINE"));Assert.assertEquals("c1",db.syncDao().state()?.changesCursor)}
}
