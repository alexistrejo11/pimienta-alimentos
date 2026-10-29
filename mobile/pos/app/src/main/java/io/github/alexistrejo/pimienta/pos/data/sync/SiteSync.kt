package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity

// How a site row is written when one projection is already stored.
internal enum class SiteWrite {
    Replace,
    ClearThenReplace,
}

// Same id replaces the row. A different id must drop the previous site first.
internal fun siteWritePlan(currentId: String?, incomingId: String): SiteWrite =
    if (currentId != null && currentId != incomingId) SiteWrite.ClearThenReplace else SiteWrite.Replace

// Replaces the stored site. Assigning the same key is the in-memory form of REPLACE.
internal fun applySiteRow(rows: MutableMap<String, SiteEntity>, site: SiteEntity) {
    when (siteWritePlan(rows.keys.firstOrNull(), site.id)) {
        SiteWrite.ClearThenReplace -> rows.clear()
        SiteWrite.Replace -> Unit
    }
    rows[site.id] = site
}
