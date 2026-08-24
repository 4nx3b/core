/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Network gatekeeper used by upstream rukamori/ArchiveTune to block unofficial
 * builds from accessing InnerTube endpoints.
 *
 * FORK NOTE: 4nx3b/ArchiveTune removed the GatekeeperRepository that would
 * normally call [setConnectionBlocked]`(false)` after a successful remote
 * verification (commit 89022ca89 "remove remote build gatekeeper"). The
 * upstream default of `connectionBlocked = true` would therefore block ALL
 * YouTube playback in our fork.
 *
 * We default to `false` here so the interceptor becomes a no-op pass-through.
 * The [setConnectionBlocked] entrypoint is kept for source compatibility —
 * it just never gets called by the fork.
 */
object NetworkGatekeeper : Interceptor {
    private val connectionBlocked = AtomicBoolean(false)

    val isConnectionBlocked: Boolean
        get() = connectionBlocked.get()

    fun setConnectionBlocked(blocked: Boolean) {
        connectionBlocked.set(blocked)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (connectionBlocked.get()) {
            throw IOException(
                "Connection blocked by ArchiveTune Remote. This app could not be verified as an " +
                    "official ArchiveTune build. Install an official build from " +
                    "https://github.com/rukamori/ArchiveTune or https://t.me/ArchiveTuneGC.",
            )
        }
        return chain.proceed(chain.request())
    }
}
