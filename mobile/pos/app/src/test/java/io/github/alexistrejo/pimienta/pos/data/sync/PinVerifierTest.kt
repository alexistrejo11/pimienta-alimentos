package io.github.alexistrejo.pimienta.pos.data.sync

import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Confirms production Argon2id (Spring defaults) and sandbox digests verify without native libs.
class PinVerifierTest {
    @Test
    fun matches_springStyleArgon2id_acceptsCorrectPin() {
        val hash = encodeSpringStyleArgon2id("12345")
        assertTrue(PinVerifier.matches("12345", hash, sandbox = false))
        assertFalse(PinVerifier.matches("00000", hash, sandbox = false))
    }

    @Test
    fun matches_argon2WithoutLeadingDollar_stillVerifies() {
        val hash = encodeSpringStyleArgon2id("2580").removePrefix("$")
        assertTrue(PinVerifier.matches("2580", hash, sandbox = false))
    }

    @Test
    fun matches_sandboxSha256_acceptsTrainingPin() {
        val hash = "3383b6e47c9df8a2ff5f39fc976ddf7ae2591fa84434bb58594eef7a45981354"
        assertTrue(PinVerifier.matches("1234", hash, sandbox = true))
        assertFalse(PinVerifier.matches("1234", hash, sandbox = false))
    }

    // Mirrors Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8() + Argon2EncodingUtils.encode.
    private fun encodeSpringStyleArgon2id(pin: String): String {
        val salt = ByteArray(16) { it.toByte() }
        val hash = ByteArray(32)
        val params =
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withParallelism(1)
                .withMemoryAsKB(1 shl 14)
                .withIterations(2)
                .build()
        Argon2BytesGenerator().apply {
            init(params)
            generateBytes(pin.toCharArray(), hash)
        }
        val b64 = Base64.getEncoder().withoutPadding()
        return "\$argon2id\$v=${params.version}\$m=${params.memory},t=${params.iterations},p=${params.lanes}\$" +
            "${b64.encodeToString(salt)}\$${b64.encodeToString(hash)}"
    }
}
