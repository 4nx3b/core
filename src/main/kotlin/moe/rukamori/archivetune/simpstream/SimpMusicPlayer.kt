/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from SimpMusic (https://github.com/maxrave-dev/SimpMusic),
 * kotlinYtmusicScraper YouTube.kt player() + newPipePlayer() —
 * GPL-3.0, © maxrave-dev. This is SimpMusic's exact stream resolution:
 *   1. InnerTube `player` request on WEB_REMIX with a random 16-char CPN and
 *      the days-since-epoch signature timestamp.
 *   2. A 3-tier NewPipe extraction (PipePipe local QuickJS decoder ->
 *      PipePipe remote api.pipepipe.dev -> BravePipe) whose resolved URLs
 *      REPLACE the InnerTube response's URLs, matched by itag.
 *   3. Manifest URLs (m3u8/mpd) are appended as extra formats.
 *   4. A random merged URL is HEAD-checked; a 4xx fails the whole attempt.
 * Adaptations: the InnerTube call goes through ArchiveTune's core YouTube
 * facade (which adds PO tokens and login context the same way the rest of
 * the app does), ktor's toKmpUri is replaced by okhttp's HttpUrl, Logger is
 * Timber, and the constructed Format carries ArchiveTune's extra `cipher`
 * field (null).
 */

package moe.rukamori.archivetune.simpstream

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.head
import moe.rukamori.archivetune.innertube.PlaybackAuthState
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.YouTubeClient.Companion.WEB_REMIX
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse
import moe.rukamori.archivetune.simpstream.extractor.ExtractSource
import moe.rukamori.archivetune.simpstream.extractor.SimpMusicExtractor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/** Song vs Video, detected the way SimpMusic does it: a square first thumbnail means a song. */
enum class MediaType {
    Song,
    Video,
}

/**
 * The ported SimpMusic resolution entry point. One object so the extractor's
 * singleton state (NewPipe.init, local decoder, player-config table) is shared
 * across every resolution, exactly like SimpMusic's DI-scoped YouTube instance.
 */
object SimpMusicPlayer {
    private const val TAG = "SimpMusicPlayer"

    private val extractor by lazy { SimpMusicExtractor() }
    private val headCheckClient by lazy { HttpClient(OkHttp) }

    @Volatile
    private var initialized = false

    /** Mirror of SimpMusic's Extractor.init() — runs once before the first extraction. */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            extractor.init()
            initialized = true
            Timber.tag(TAG).d("SimpMusic extractor initialized (PipePipe + BravePipe)")
        }
    }

    /** Mirror of SimpMusic's Extractor.logIn(cookie). */
    fun logIn(cookie: String?) = extractor.logIn(cookie)

    /** Mirror of SimpMusic's getExtractSource(videoId). */
    fun getExtractSource(videoId: String): String? = ExtractSource.of(videoId)

    suspend fun is403Url(url: String): Boolean =
        try {
            headCheckClient.head(url).status.value in 400..499
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "is403Url HEAD check failed")
            true
        }

    fun isManifestUrl(url: String): Boolean = url.contains(".m3u8") || url.contains(".mpd") || url.contains("manifest")

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<Triple<String?, PlayerResponse, MediaType>> =
        runCatching {
            ensureInitialized()
            // SimpMusic passes the account cookie to the extractors so they can
            // see age-gated / members-only videos; anonymous users pass "".
            logIn(authState.cookie)

            val cpn =
                (1..16)
                    .map {
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[
                            Random.nextInt(
                                0,
                                64,
                            ),
                        ]
                    }.joinToString("")

            var decodedSigResponse: PlayerResponse? = null
            val tempRes =
                YouTube
                    .player(
                        videoId = videoId,
                        playlistId = playlistId,
                        client = WEB_REMIX,
                        signatureTimestamp = currentSignatureTimestamp(),
                        setLogin = true,
                        authState = authState,
                        cpn = cpn,
                    ).getOrThrow()
                    .let(::withFexpEnrichedTracking)

            val response = newPipePlayer(videoId, tempRes)
            if (response != null) {
                decodedSigResponse = response
                Timber.tag(TAG).d("player: NewPipe URLs merged for %s", videoId)
            } else {
                Timber.tag(TAG).w("player: no NewPipe URL found for %s", videoId)
            }
            if (decodedSigResponse == null) throw RuntimeException("No URL found")
            val firstThumb =
                decodedSigResponse.videoDetails
                    ?.thumbnail
                    ?.thumbnails
                    ?.firstOrNull()
            val thumbnails =
                if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
            return@runCatching Triple(
                cpn,
                decodedSigResponse.copy(
                    videoDetails = decodedSigResponse.videoDetails?.copy(),
                    playbackTracking = decodedSigResponse.playbackTracking,
                ),
                thumbnails,
            )
        }

    /**
     * SimpMusic's newPipePlayer: run the 3-tier extraction and splice its URLs into the
     * InnerTube response, matched by itag. Manifest URLs become extra formats; the HLS
     * manifest (itag 96) lands in streamingData.hlsManifestUrl. A random merged URL is
     * HEAD-checked — a 4xx fails the whole attempt so the caller can fall back.
     */
    suspend fun newPipePlayer(
        videoId: String,
        tempRes: PlayerResponse,
    ): PlayerResponse? {
        val listUrlSig = mutableListOf<String>()
        var decodedSigResponse: PlayerResponse?
        val sigResponse: PlayerResponse
        Timber.tag(TAG).d("tempRes playabilityStatus: ${tempRes.playabilityStatus}")
        if (tempRes.playabilityStatus.status != "OK") {
            return null
        } else {
            sigResponse = tempRes
        }
        val streamsList = extractor.newPipePlayer(videoId)
        if (streamsList.isEmpty()) return null

        decodedSigResponse =
            sigResponse.copy(
                streamingData =
                    sigResponse.streamingData?.copy(
                        formats =
                            sigResponse.streamingData.formats?.map { format ->
                                format.copy(
                                    url = streamsList.find { it.first == format.itag }?.second,
                                )
                            },
                        adaptiveFormats =
                            sigResponse.streamingData.adaptiveFormats.map { adaptiveFormats ->
                                adaptiveFormats.copy(
                                    url = streamsList.find { it.first == adaptiveFormats.itag }?.second,
                                )
                            },
                        hlsManifestUrl = streamsList.firstOrNull { it.first == 96 }?.second,
                    ),
            )
        decodedSigResponse =
            decodedSigResponse.copy(
                streamingData =
                    decodedSigResponse.streamingData?.copy(
                        formats =
                            decodedSigResponse.streamingData.formats?.let { formats ->
                                val copy = formats.toMutableList()
                                streamsList
                                    .filter {
                                        isManifestUrl(it.second)
                                    }.forEach { manifest ->
                                        copy.add(
                                            PlayerResponse.StreamingData.Format(
                                                itag = manifest.first,
                                                url = manifest.second,
                                                mimeType = "",
                                                bitrate = 0,
                                                width = if (manifest.first == 96) 1920 else 1080,
                                                height = if (manifest.first == 96) 1080 else 720,
                                                contentLength = 0,
                                                quality = "",
                                                fps = 0,
                                                qualityLabel = "",
                                                averageBitrate = 0,
                                                audioQuality = "",
                                                approxDurationMs = "",
                                                audioSampleRate = 0,
                                                audioChannels = 0,
                                                loudnessDb = 0.0,
                                                lastModified = 0,
                                                signatureCipher = "",
                                                cipher = null,
                                            ),
                                        )
                                    }
                                copy
                            },
                    ),
            )
        listUrlSig.addAll(
            (
                decodedSigResponse
                    .streamingData
                    ?.adaptiveFormats
                    ?.mapNotNull { it.url }
                    ?.toMutableList() ?: mutableListOf()
            ).apply {
                decodedSigResponse
                    .streamingData
                    ?.formats
                    ?.mapNotNull { it.url }
                    ?.let { addAll(it) }
            },
        )
        val randomUrl = listUrlSig.randomOrNull() ?: return null
        if (listUrlSig.isNotEmpty() && !is403Url(randomUrl)) {
            Timber.tag(TAG).d("NewPipe found working URL (itag-merged) for %s", videoId)
            return decodedSigResponse
        } else {
            Timber.tag(TAG).w("NewPipe URL HEAD check failed for %s", videoId)
            return null
        }
    }

    /**
     * SimpMusic computes the signature timestamp as whole days since the Unix epoch
     * (kotlinx-datetime's epoch.daysUntil(today)); java.time computes the same number.
     */
    private fun currentSignatureTimestamp(): Int =
        ChronoUnit.DAYS.between(LocalDate.of(1970, 1, 1), LocalDate.now(ZoneOffset.UTC)).toInt()

    /**
     * SimpMusic reads the `fexp` parameter off streamingData.serverAbrStreamingUrl and appends
     * it to the three playback-tracking base URLs — the watch-time reporting YouTube expects.
     */
    private fun withFexpEnrichedTracking(response: PlayerResponse): PlayerResponse {
        val fexp =
            response.streamingData
                ?.serverAbrStreamingUrl
                ?.toHttpUrlOrNull()
                ?.queryParameter("fexp")
        val playbackTracking = response.playbackTracking
        return response.copy(
            playbackTracking =
                playbackTracking?.copy(
                    atrUrl =
                        playbackTracking.atrUrl?.copy(
                            baseUrl =
                                playbackTracking.atrUrl.baseUrl
                                    ?.toHttpUrlOrNull()
                                    ?.newBuilder()
                                    ?.apply {
                                        if (fexp != null) {
                                            addQueryParameter("fexp", fexp)
                                        }
                                    }?.build()
                                    ?.toString(),
                        ),
                    videostatsPlaybackUrl =
                        playbackTracking.videostatsPlaybackUrl?.copy(
                            baseUrl =
                                playbackTracking.videostatsPlaybackUrl.baseUrl
                                    ?.toHttpUrlOrNull()
                                    ?.newBuilder()
                                    ?.apply {
                                        if (fexp != null) {
                                            addQueryParameter("fexp", fexp)
                                        }
                                    }?.build()
                                    ?.toString(),
                        ),
                    videostatsWatchtimeUrl =
                        playbackTracking.videostatsWatchtimeUrl?.copy(
                            baseUrl =
                                playbackTracking.videostatsWatchtimeUrl.baseUrl
                                    ?.toHttpUrlOrNull()
                                    ?.newBuilder()
                                    ?.apply {
                                        if (fexp != null) {
                                            addQueryParameter("fexp", fexp)
                                        }
                                    }?.build()
                                    ?.toString(),
                        ),
                ),
        )
    }
}
