package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity

// Outcome of a local rename plus the attempt to PATCH the device.
internal sealed class DeviceRenameResult {
    data object Invalid : DeviceRenameResult()
    data object Synced : DeviceRenameResult()
    data object Pending : DeviceRenameResult()
}

internal const val DEVICE_CONFIG_PREFS = "pos-device-config"
internal const val PENDING_DEVICE_NAME_KEY = "pendingDeviceName"
internal const val DEVICE_NAME_MAX_LENGTH = 40
internal const val DEVICE_NAME_UPDATED_MESSAGE = "Nombre actualizado"
internal const val DEVICE_NAME_PENDING_MESSAGE =
    "Guardado en esta tablet; pendiente de sincronizar con el servidor."
internal const val DEVICE_NAME_INVALID_MESSAGE = "Escribe un nombre de hasta 40 caracteres."

// Trimmed name accepted by enroll and PATCH /devices/me. Null when the text cannot be saved.
internal fun normalizeDeviceName(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.length > DEVICE_NAME_MAX_LENGTH) return null
    return trimmed
}

internal fun localDeviceAfterRename(device: DeviceEntity, normalized: String): DeviceEntity =
    device.copy(name = normalized)

// Server name wins after a successful PATCH. A blank response keeps the local name.
internal fun deviceAfterConfirmedRename(device: DeviceEntity, canonical: String): DeviceEntity {
    val name = normalizeDeviceName(canonical) ?: device.name
    return device.copy(name = name)
}

// Bootstrap writes the server snapshot, then a still-pending local rename is put back.
internal fun deviceAfterBootstrap(device: DeviceEntity, snapshotName: String, pending: String?): DeviceEntity {
    val kept = pending?.let(::normalizeDeviceName)
    return device.copy(name = kept ?: snapshotName)
}
