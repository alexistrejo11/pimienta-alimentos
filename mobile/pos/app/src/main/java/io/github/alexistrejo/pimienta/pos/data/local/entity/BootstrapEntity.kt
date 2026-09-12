package io.github.alexistrejo.pimienta.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Records applied snapshots so the same bootstrap is not imported twice.
@Entity(tableName = "bootstrap_snapshot")
data class BootstrapEntity(@PrimaryKey val snapshotId: String, val schemaVersion: Int, val appliedAtEpochMillis: Long)
