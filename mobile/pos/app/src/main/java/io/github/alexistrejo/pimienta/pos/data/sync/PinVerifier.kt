package io.github.alexistrejo.pimienta.pos.data.sync

import java.security.MessageDigest
import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

// Verifies operator PINs against Spring Argon2id hashes (pure Java) or sandbox SHA-256 digests.
object PinVerifier {
    private val b64 = Base64.getDecoder()

    fun matches(pin: String, storedHash: String, sandbox: Boolean): Boolean {
        val hash = storedHash.trim()
        if (hash.startsWith("\$argon2") || hash.startsWith("argon2id")) {
            return verifyArgon2(pin, normalizeArgon2Hash(hash))
        }
        if (sandbox) return debugSha256(pin) == hash
        return false
    }

    // Recomputes Argon2id with the salt/params embedded in the PHC string (same path as Spring Security).
    private fun verifyArgon2(pin: String, encodedHash: String): Boolean {
        val decoded = runCatching { decodeArgon2(encodedHash) }.getOrNull() ?: return false
        val actual = ByteArray(decoded.hash.size)
        val generator = Argon2BytesGenerator()
        generator.init(decoded.parameters)
        generator.generateBytes(pin.toCharArray(), actual)
        return constantTimeEquals(decoded.hash, actual)
    }

    // Parses `$argon2id$v=19$m=...,t=...,p=...$salt$hash` exactly like Spring's Argon2EncodingUtils.
    private fun decodeArgon2(encodedHash: String): DecodedArgon2 {
        val parts = encodedHash.split("$")
        require(parts.size >= 5) { "Invalid encoded Argon2-hash" }
        var i = 1
        val type =
            when (parts[i++]) {
                "argon2d" -> Argon2Parameters.ARGON2_d
                "argon2i" -> Argon2Parameters.ARGON2_i
                "argon2id" -> Argon2Parameters.ARGON2_id
                else -> error("Invalid algorithm type: ${parts[1]}")
            }
        val builder = Argon2Parameters.Builder(type)
        if (parts[i].startsWith("v=")) {
            builder.withVersion(parts[i].substring(2).toInt())
            i++
        }
        val performance = parts[i++].split(",")
        require(performance.size == 3) { "Amount of performance parameters invalid" }
        require(performance[0].startsWith("m=")) { "Invalid memory parameter" }
        require(performance[1].startsWith("t=")) { "Invalid iterations parameter" }
        require(performance[2].startsWith("p=")) { "Invalid parallelity parameter" }
        builder.withMemoryAsKB(performance[0].substring(2).toInt())
        builder.withIterations(performance[1].substring(2).toInt())
        builder.withParallelism(performance[2].substring(2).toInt())
        builder.withSalt(b64.decode(parts[i++]))
        return DecodedArgon2(b64.decode(parts[i]), builder.build())
    }

    // Spring and device contracts may omit the leading dollar sign on Argon2 encodings.
    private fun normalizeArgon2Hash(hash: String): String =
        if (hash.startsWith("argon2id") && !hash.startsWith("\$")) "\$$hash" else hash

    // Sandbox bootstrap uses a deterministic SHA-256 digest instead of Argon2id.
    private fun debugSha256(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("pimienta-debug|$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(expected: ByteArray, actual: ByteArray): Boolean {
        if (expected.size != actual.size) return false
        var result = 0
        for (i in expected.indices) result = result or (expected[i].toInt() xor actual[i].toInt())
        return result == 0
    }

    private data class DecodedArgon2(val hash: ByteArray, val parameters: Argon2Parameters)
}
