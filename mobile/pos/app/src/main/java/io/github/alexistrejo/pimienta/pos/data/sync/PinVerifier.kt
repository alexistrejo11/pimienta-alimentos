package io.github.alexistrejo.pimienta.pos.data.sync

import de.mkammerer.argon2.Argon2Factory
import java.security.MessageDigest

// Verifies operator PINs against server Argon2id hashes or sandbox debug digests.
object PinVerifier {
    // Lazily initialized to prevent UnsatisfiedLinkError on emulators when operating in sandbox mode.
    private val argon2 by lazy { Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id) }

    fun matches(pin: String, storedHash: String, sandbox: Boolean): Boolean {
        val hash = storedHash.trim()
        if (hash.startsWith("\$argon2") || hash.startsWith("argon2id")) {
            return runCatching { argon2.verify(normalizeArgon2Hash(hash), pin.toCharArray()) }.getOrDefault(false)
        }
        if (sandbox) return debugSha256(pin) == hash
        return false
    }

    // Spring and device contracts may omit the leading dollar sign on Argon2 encodings.
    private fun normalizeArgon2Hash(hash: String): String = if (hash.startsWith("argon2id") && !hash.startsWith("\$")) "\$$hash" else hash

    // Sandbox bootstrap uses a deterministic SHA-256 digest instead of Argon2id.
    private fun debugSha256(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("pimienta-debug|$pin".toByteArray()).joinToString("") { "%02x".format(it) }
}
