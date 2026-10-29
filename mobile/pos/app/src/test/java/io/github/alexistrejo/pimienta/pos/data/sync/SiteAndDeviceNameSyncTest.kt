package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import io.github.alexistrejo.pimienta.pos.data.printing.saleTicket
import io.github.alexistrejo.pimienta.pos.hardware.EscPosEncoder
import io.github.alexistrejo.pimienta.pos.hardware.PrintableLine
import java.nio.charset.Charset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Site snapshot and device rename rules that Room and the sync pipeline apply.
class SiteAndDeviceNameSyncTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val enrolled = SiteEntity("1", "Colegio Largo Nombre", "Dirección vieja", "MXN")

    @Test
    fun changesSiteReplacesExistingRowWithoutPrimaryKeyAbort() {
        val rows = linkedMapOf(enrolled.id to enrolled)
        assertThrows(IllegalStateException::class.java) {
            insertAborting(rows, enrolled.copy(name = "CNA"))
        }
        assertEquals("Colegio Largo Nombre", rows.getValue("1").name)

        val updated = SiteEntity("1", "CNA", "Calle 1", "MXN")
        applySiteRow(rows, updated)

        assertEquals(1, rows.size)
        assertEquals("CNA", rows.getValue("1").name)
        assertEquals("Calle 1", rows.getValue("1").address)
        assertEquals("MXN", rows.getValue("1").currency)
    }

    @Test
    fun siteOperationAndSnapshotBothReplaceTheEnrolledSite() {
        val fromOperation = linkedMapOf(enrolled.id to enrolled)
        applySiteRow(fromOperation, SiteDto("1", "CNA", "Calle 1", "MXN").toSite())
        assertEquals("CNA", fromOperation.values.single().name)

        val decoded = json.decodeFromString(
            ChangesResponse.serializer(),
            """
            {"schemaVersion":1,"nextCursor":"cursor-hq-1-s9","operations":[],"site":{"id":"1","name":"CNA","address":"Calle 1","currency":"MXN"}}
            """.trimIndent(),
        )
        val fromSnapshot = linkedMapOf(enrolled.id to enrolled)
        applySiteRow(fromSnapshot, decoded.site!!.toSite())
        assertEquals("Calle 1", fromSnapshot.values.single().address)

        val legacy = json.decodeFromString(
            ChangesResponse.serializer(),
            """{"schemaVersion":1,"nextCursor":"cursor-hq-1-s9","operations":[]}""",
        )
        assertNull(legacy.site)
    }

    @Test
    fun ticketUsesSiteInitialsAndAddressRightAfterTheSync() {
        val rows = linkedMapOf(enrolled.id to enrolled)
        applySiteRow(rows, SiteEntity("1", "CNA", "Calle 1", "MXN"))
        val site = rows.values.single()
        val sale = SaleEntity(
            "sale-1",
            "T1-0001",
            "shift-1",
            "cashier-1",
            1500,
            0,
            1500,
            "CASH",
            2000,
            500,
            1_700_000_000_000L,
        )
        val ticket = saleTicket(
            sale = sale,
            lines = listOf(PrintableLine("Agua", "1", 1500)),
            siteName = site.name,
            siteAddress = site.address,
            duplicate = false,
            isCash = true,
        )

        assertEquals("CNA", ticket.siteName)
        assertEquals("Calle 1", ticket.siteAddress)
        val text = EscPosEncoder().encode(ticket).toString(Charset.forName("CP850"))
        assertTrue(text.contains("CNA"))
        assertTrue(text.contains("Calle 1"))
    }

    @Test
    fun onlineRenameClearsPendingAndKeepsCanonicalName() {
        val tablet = TabletName(device("tablet android"))
        assertEquals(DeviceRenameResult.Pending, tablet.saveLocally("  Caja Norte  "))
        assertEquals("Caja Norte", tablet.device.name)
        assertEquals("Caja Norte", tablet.pending)

        tablet.patchSucceeded("Caja Norte")

        assertNull(tablet.pending)
        assertEquals("Caja Norte", tablet.device.name)
    }

    @Test
    fun offlineRenameKeepsPendingName() {
        val tablet = TabletName(device("tablet android"))
        tablet.saveLocally("Caja Norte")
        tablet.patchFailed()

        assertEquals("Caja Norte", tablet.pending)
        assertEquals("Caja Norte", tablet.device.name)
    }

    @Test
    fun bootstrapDoesNotRevertAPendingRename() {
        val tablet = TabletName(device("tablet android"))
        tablet.saveLocally("Caja Norte")
        tablet.patchFailed()

        tablet.bootstrap(snapshotName = "tablet android", patchSucceeded = false)

        assertEquals("Caja Norte", tablet.device.name)
        assertEquals("Caja Norte", tablet.pending)
    }

    @Test
    fun bootstrapKeepsServerNameAfterTheRenameWasSynced() {
        val tablet = TabletName(device("tablet android"))
        tablet.saveLocally("Caja Norte")
        tablet.patchSucceeded("Caja Norte")

        tablet.bootstrap(snapshotName = "Caja Norte", patchSucceeded = true)

        assertNull(tablet.pending)
        assertEquals("Caja Norte", tablet.device.name)
    }

    @Test
    fun blankOrTooLongNameIsRejectedBeforeAnythingIsStored() {
        val tablet = TabletName(device("tablet android"))
        assertEquals(DeviceRenameResult.Invalid, tablet.saveLocally("   "))
        assertEquals(DeviceRenameResult.Invalid, tablet.saveLocally("x".repeat(41)))
        assertEquals("tablet android", tablet.device.name)
        assertNull(tablet.pending)
        assertEquals("Caja", normalizeDeviceName("Caja"))
        assertEquals(40, normalizeDeviceName("x".repeat(40))?.length)
    }

    // Models an insert that aborts on a duplicate primary key, which Room did before REPLACE.
    private fun insertAborting(rows: MutableMap<String, SiteEntity>, site: SiteEntity) {
        if (site.id in rows) throw IllegalStateException("PRIMARY KEY constraint failed")
        rows[site.id] = site
    }

    private fun device(name: String) = DeviceEntity("dev-1", name, "T1", 1, "1", "AUTHORIZED")

    // Local prefs + device row, in the same order the repository and sync pipeline use.
    private class TabletName(var device: DeviceEntity) {
        var pending: String? = null

        fun saveLocally(raw: String): DeviceRenameResult {
            val name = normalizeDeviceName(raw) ?: return DeviceRenameResult.Invalid
            if (name == device.name && pending == null) return DeviceRenameResult.Synced
            device = localDeviceAfterRename(device, name)
            pending = name
            return DeviceRenameResult.Pending
        }

        fun patchSucceeded(canonical: String) {
            device = deviceAfterConfirmedRename(device, canonical)
            pending = null
        }

        fun patchFailed() = Unit

        fun bootstrap(snapshotName: String, patchSucceeded: Boolean) {
            device = deviceAfterBootstrap(device, snapshotName, if (patchSucceeded) null else pending)
        }
    }
}
