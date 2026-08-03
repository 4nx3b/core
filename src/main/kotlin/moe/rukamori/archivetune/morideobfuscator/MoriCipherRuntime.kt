/*
* ArchiveTune (2026)
* © Rukamori — github.com/rukamori
* GPL-3.0 License | Contributors: see git history
* Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
*/

package moe.rukamori.archivetune.morideobfuscator

/**
 * Stub used when the proprietary morideobfuscator module is absent.
 * All methods return Result.failure so callers automatically fall back
 * to the NewPipe / JavaScript-player path — no functional change needed.
 */
object MoriCipherRuntime {
    fun signatureTimestamp(videoId: String): Result<Int> =
        Result.failure(UnsupportedOperationException("MoriCipherRuntime stub"))

    fun transformNParameter(videoId: String, url: String): Result<String> =
        Result.failure(UnsupportedOperationException("MoriCipherRuntime stub"))

    fun resolveStreamUrl(videoId: String, cipherString: String): Result<String> =
        Result.failure(UnsupportedOperationException("MoriCipherRuntime stub"))
}
