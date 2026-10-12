package io.github.alexistrejo.pimienta.pos.data.sync

/**
 * Minimal persistence contract for the device's API session tokens.
 *
 * Extracted so [DeviceSession] can be unit-tested without the Android Keystore
 * backed [DeviceCredentials]. Production code keeps using DeviceCredentials;
 * this interface only exists to allow a fake in tests.
 */
interface DeviceTokenStore {
    fun access(): String?
    fun refresh(): String?
    fun save(access: String, refresh: String)
    fun clear()
}
