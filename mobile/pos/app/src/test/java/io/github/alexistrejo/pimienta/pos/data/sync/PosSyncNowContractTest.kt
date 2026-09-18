package io.github.alexistrejo.pimienta.pos.data.sync

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The explicit sync button awaits PosSyncPipeline; it must not enqueue WorkManager.
class PosSyncNowContractTest {
    @Test
    fun foregroundSyncAwaitsPipelineWithoutWorkManager() {
        val file = File("src/main/java/io/github/alexistrejo/pimienta/pos/data/sync/ForegroundSync.kt")
        assertTrue(file.exists())
        val text = file.readText()
        assertTrue(text.contains("PosSyncPipeline"))
        assertFalse(text.contains("WorkManager"))
        assertFalse(text.contains("SyncWorker"))
    }
}
