package io.github.alexistrejo.pimienta.pos.data.update

/** Pure versionCode comparison used by the release checker and unit tests. */
object VersionCompare {
    fun isNewer(remoteVersionCode: Int, localVersionCode: Int): Boolean =
        remoteVersionCode > localVersionCode
}
