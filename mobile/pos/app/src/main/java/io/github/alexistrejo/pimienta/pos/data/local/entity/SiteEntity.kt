package io.github.alexistrejo.pimienta.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Stores the active site projection from a bootstrap snapshot.
@Entity(tableName = "site")
data class SiteEntity(@PrimaryKey val id: String, val name: String, val address: String, val currency: String)
