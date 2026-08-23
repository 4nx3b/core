package moe.rukamori.archivetune.innertube

import moe.rukamori.archivetune.innertube.models.MediaInfo
import moe.rukamori.archivetune.innertube.models.ReturnYouTubeDislikeResponse
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import moe.rukamori.archivetune.innertube.models.response.NextResponse
<<<<<<< HEAD
import moe.rukamori.archivetune.innertubex.InnerTube as InnerTubeX
import moe.rukamori.archivetune.innertubex.InnerTubeHttpException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
=======
import moe.rukamori.archivetune.innertube.utils.sha1
import moe.rukamori.archivetune.innertube.utils.youtubeLoginCookieValue
import okhttp3.Dns
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0
import java.io.IOException
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Compatibility facade that keeps Metrolist's parsed response models while InnerTubeX owns
 * YouTube request construction, session handling, retries, and authenticated mutations.
 */
class InnerTube {
    private var configuredProxy: Proxy? = null
    private var configuredProxyAuth: String? = null
    private var httpClient = createClient()
    private var innerTubeX = InnerTubeX(httpClient)

    var locale: YouTubeLocale
        get() = innerTubeX.locale
        set(value) {
            innerTubeX.locale = value
        }

    var visitorData: String?
        get() = innerTubeX.visitorData
        set(value) {
            innerTubeX.visitorData = value
        }

    var dataSyncId: String?
        get() = innerTubeX.dataSyncId
        set(value) {
            innerTubeX.dataSyncId = value
        }

    var authUser: String
        get() = innerTubeX.authUser
        set(value) {
            innerTubeX.authUser = value
        }

    var cookie: String?
        get() = innerTubeX.cookie
        set(value) {
            innerTubeX.cookie = value
        }

    var proxy: Proxy?
        get() = configuredProxy
        set(value) {
            if (configuredProxy == value) return
            configuredProxy = value
            recreateTransport()
        }

    var proxyAuth: String?
        get() = configuredProxyAuth
        set(value) {
            if (configuredProxyAuth == value) return
            configuredProxyAuth = value
            if (configuredProxy != null) recreateTransport()
        }

    var useLoginForBrowse: Boolean
        get() = innerTubeX.useLoginForBrowse
        set(value) {
            innerTubeX.useLoginForBrowse = value
        }

<<<<<<< HEAD
    private fun recreateTransport() {
        val session = innerTubeX.sessionSnapshot()
        innerTubeX.close()
        httpClient.close()
        httpClient = createClient()
        innerTubeX =
            InnerTubeX(httpClient).also { replacement ->
                replacement.locale = session.locale
                replacement.replaceSession(
                    cookie = session.cookie,
                    visitorData = session.visitorData,
                    dataSyncId = session.dataSyncId,
                    authUser = session.authUser,
                    useLoginForBrowse = session.useLoginForBrowse,
                )
                replacement.regionOverrideActive = session.regionOverrideActive
            }
=======
    var dns: Dns = Dns.SYSTEM
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var useLoginForBrowse: Boolean = false

    fun currentAuthState(): PlaybackAuthState = authState

    fun applyAuthState(value: PlaybackAuthState) {
        authState = value.normalized()
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() =
        HttpClient(OkHttp) {
            // InnerTubeX handles endpoint-specific status validation and transient retries.
            expectSuccess = false

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }

            install(ContentEncoding) {
                gzip(0.9F)
                deflate(0.8F)
            }

            engine {
                config {
<<<<<<< HEAD
                    connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                    protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
                    retryOnConnectionFailure(true)
                    cache(okhttp3.Cache(File(System.getProperty("java.io.tmpdir"), "http_cache"), 50L * 1024L * 1024L))
                    configuredProxy?.let(::proxy)
                    configuredProxyAuth?.let { auth ->
=======
                    addInterceptor(NetworkGatekeeper)
                    dns(this@InnerTube.dns)
                    if (this@InnerTube.proxy == null) {
                        proxy(Proxy.NO_PROXY)
                    } else if (this@InnerTube.proxy != null && !proxyUsername.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0
                        proxyAuthenticator { _, response ->
                            response.request
                                .newBuilder()
                                .header("Proxy-Authorization", auth)
                                .build()
                        }
                    }
                }
<<<<<<< HEAD
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
=======
                if (this@InnerTube.proxy != null) {
                    proxy = this@InnerTube.proxy
                }
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0
            }

            defaultRequest {
                url("https://music.youtube.com/youtubei/v1/")
                header("Accept", "application/json")
                header("Cache-Control", "no-cache")
            }
        }

<<<<<<< HEAD
=======
    private fun HttpRequestBuilder.ytClient(
        client: YouTubeClient,
        setLogin: Boolean = false,
        authState: PlaybackAuthState = currentAuthState(),
        includeVisitorData: Boolean = true,
    ) {
        val requestOrigin = client.requestOrigin()
        val requestReferer = client.requestReferer()
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", requestOrigin)
            append("Referer", requestReferer)
            if (includeVisitorData) {
                authState.visitorData?.let { append("X-Goog-Visitor-Id", it) }
            }
            if (setLogin && client.supportsCookieAuthentication) {
                authState.cookie?.let { cookie ->
                    append("cookie", cookie)
                    val loginCookieValue = youtubeLoginCookieValue(cookie) ?: return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime $loginCookieValue $requestOrigin")
                    append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
                    append("X-Goog-AuthUser", "0")
                    authState.dataSyncId.delegatedSessionIdOrNull()?.let { append("X-Goog-PageId", it) }
                }
            }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    private fun HttpRequestBuilder.ytPlaybackTrackingClient(
        client: YouTubeClient,
        authState: PlaybackAuthState = currentAuthState(),
    ) {
        val requestOrigin = client.requestOrigin()
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            append(HTTP_HEADER_ACCEPT_LANGUAGE, PLAYBACK_TELEMETRY_ACCEPT_LANGUAGE)
            append(HTTP_HEADER_CACHE_CONTROL, PLAYBACK_TELEMETRY_CACHE_CONTROL)
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", requestOrigin)
            append("Referer", client.requestReferer())
            authState.visitorData?.let { append("X-Goog-Visitor-Id", it) }
            if (client.supportsCookieAuthentication) {
                authState.cookie?.let { cookie ->
                    append("cookie", cookie)
                    val loginCookieValue = youtubeLoginCookieValue(cookie) ?: return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime $loginCookieValue $requestOrigin")
                    append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
                    append("X-Goog-AuthUser", "0")
                    authState.dataSyncId.delegatedSessionIdOrNull()?.let { append("X-Goog-PageId", it) }
                }
            }
        }
        userAgent(client.userAgent)
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Throwable) {
                if (e is CancellationException || !e.isTransientNetworkFailure()) throw e
                attempt++
                if (attempt >= maxAttempts) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    private fun String?.delegatedSessionIdOrNull(): String? {
        val value = this?.trim()?.takeIf(String::isNotBlank) ?: return null
        val separatorIndex = value.indexOf("||")
        if (separatorIndex <= 0 || separatorIndex + 2 >= value.length) return null
        return value.substring(0, separatorIndex).trim().takeIf(String::isNotBlank)
    }

    private fun Throwable.isTransientNetworkFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException || current is HttpRequestTimeoutException) return true
            if (current.message?.contains("Request timeout has expired", ignoreCase = true) == true) return true
            current = current.cause
        }
        return false
    }

>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0
    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ) = innerTubeX.search(client, query, params, continuation)

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String? = null,
<<<<<<< HEAD
    ) = innerTubeX.player(client, videoId, playlistId, signatureTimestamp, poToken)
=======
        setLogin: Boolean = true,
        authState: PlaybackAuthState = currentAuthState(),
    ) = withRetry {
        val includeDataSyncId = setLogin && client.supportsCookieAuthentication && authState.hasPlaybackLoginContext
        try {
            executePlayerRequest(
                client = client,
                videoId = videoId,
                playlistId = playlistId,
                signatureTimestamp = signatureTimestamp,
                poToken = poToken,
                setLogin = setLogin,
                authState = authState,
                includeDataSyncId = includeDataSyncId,
            )
        } catch (failure: Throwable) {
            if (!shouldRetryPlayerRequestWithoutDataSyncId(failure, includeDataSyncId)) throw failure
            executePlayerRequest(
                client = client,
                videoId = videoId,
                playlistId = playlistId,
                signatureTimestamp = signatureTimestamp,
                poToken = poToken,
                setLogin = setLogin,
                authState = authState,
                includeDataSyncId = false,
            )
        }
    }

    private suspend fun executePlayerRequest(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String?,
        setLogin: Boolean,
        authState: PlaybackAuthState,
        includeDataSyncId: Boolean,
    ) = httpClient.post(client.requestApiUrl("player")) {
        ytClient(client = client, setLogin = setLogin, authState = authState)
        setBody(
            PlayerBody(
                context =
                    client
                        .toContext(
                            locale = locale,
                            visitorData = authState.visitorData,
                            dataSyncId = if (includeDataSyncId) authState.dataSyncId else null,
                        ).let {
                            if (client.isEmbedded) {
                                it.copy(
                                    thirdParty =
                                        Context.ThirdParty(
                                            embedUrl = "https://www.reddit.com/",
                                        ),
                                )
                            } else {
                                it
                            }
                        },
                videoId = videoId,
                playlistId = playlistId,
                playbackContext =
                    if (client.useSignatureTimestamp) {
                        PlayerBody.PlaybackContext(
                            PlayerBody.PlaybackContext.ContentPlaybackContext(
                                signatureTimestamp,
                            ),
                        )
                    } else {
                        null
                    },
                serviceIntegrityDimensions =
                    poToken?.let {
                        PlayerBody.ServiceIntegrityDimensions(poToken = it)
                    },
            ),
        )
    }

    private fun shouldRetryPlayerRequestWithoutDataSyncId(
        failure: Throwable,
        includeDataSyncId: Boolean,
    ): Boolean {
        if (!includeDataSyncId) return false
        val clientError = failure as? ClientRequestException ?: return false
        if (clientError.response.status != HttpStatusCode.BadRequest) return false
        val message = clientError.message.orEmpty()
        if (!message.contains("/youtubei/v1/player", ignoreCase = true)) return false
        if (message.contains("Origin doesn't match Host", ignoreCase = true)) return false
        return message.contains("INVALID_ARGUMENT", ignoreCase = true) ||
            message.contains("invalid argument", ignoreCase = true)
    }
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
    ) = innerTubeX.registerPlayback(client, url, cpn, playlistId).requireSuccess("registerPlayback")

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
<<<<<<< HEAD
    ) = innerTubeX.browse(client, browseId, params, continuation, setLogin)
=======
        useAccountContext: Boolean = true,
    ) = withRetry {
        httpClient.post("browse") {
            val shouldUseLogin = useAccountContext && (setLogin || useLoginForBrowse)
            ytClient(
                client = client,
                setLogin = shouldUseLogin,
                includeVisitorData = useAccountContext,
            )
            setBody(
                BrowseBody(
                    context =
                        client.toContext(
                            locale,
                            if (useAccountContext) visitorData else null,
                            if (shouldUseLogin) dataSyncId else null,
                        ),
                    browseId = browseId,
                    params = params,
                    continuation = continuation,
                ),
            )
        }
    }
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = innerTubeX.next(client, videoId, playlistId, playlistSetVideoId, index, params, continuation)

    suspend fun feedback(
        client: YouTubeClient,
        tokens: List<String>,
    ) = innerTubeX.feedback(client, tokens).requireSuccess("feedback")

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = innerTubeX.getSearchSuggestions(client, input)

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = innerTubeX.getQueue(client, videoIds, playlistId)

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
<<<<<<< HEAD
    ) = innerTubeX.getTranscript(client, videoId)
=======
        authState: PlaybackAuthState = currentAuthState(),
        poToken: String? = null,
    ) = withRetry {
        httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            poToken?.let {
                parameter("pot", it)
                parameter("potc", "1")
                parameter("c", client.clientId)
            }
            ytClient(
                client = client,
                setLogin = authState.hasPlaybackLoginContext,
                authState = authState,
            )
            setBody(
                GetTranscriptBody(
                    context =
                        client.toContext(
                            locale = locale,
                            visitorData = authState.visitorData,
                            dataSyncId = if (authState.hasPlaybackLoginContext) authState.dataSyncId else null,
                        ),
                    params =
                        Base64.Default.encode(
                            "\n${11.toChar()}$videoId".encodeToByteArray(),
                        ),
                ),
            )
        }
    }
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0

    suspend fun fetchFreshVisitorData() = innerTubeX.fetchFreshVisitorData()

    suspend fun accountMenu(client: YouTubeClient) = innerTubeX.accountMenu(client).requireSuccess("accountMenu")

<<<<<<< HEAD
    suspend fun accountsList() = innerTubeX.accountsList(YouTubeClient.WEB).requireSuccess("accountsList")
=======
    suspend fun accountChannels(client: YouTubeClient) =
        withRetry {
            httpClient.post(client.requestApiUrl("account/accounts_list")) {
                ytClient(client, setLogin = true)
                setBody(AccountsListBody(client.toContext(locale, visitorData, dataSyncId)))
            }
        }
>>>>>>> 7f5da59f876e65a1ead52667559b187270fceab0

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = innerTubeX.likeVideo(client, videoId).requireSuccess("likeVideo")

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = innerTubeX.unlikeVideo(client, videoId).requireSuccess("unlikeVideo")

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = innerTubeX.subscribeChannel(client, channelId, params).requireSuccess("subscribeChannel")

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = innerTubeX.unsubscribeChannel(client, channelId, params).requireSuccess("unsubscribeChannel")

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = innerTubeX.likePlaylist(client, playlistId).requireSuccess("likePlaylist")

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = innerTubeX.unlikePlaylist(client, playlistId).requireSuccess("unlikePlaylist")

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = innerTubeX.addToPlaylist(client, playlistId, videoId).requireSuccess("addToPlaylist")

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addedPlaylistId: String,
    ) = innerTubeX.addPlaylistToPlaylist(client, playlistId, addedPlaylistId).requireSuccess("addPlaylistToPlaylist")

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = innerTubeX.removePlaylistSong(client, playlistId, setVideoId, videoId).requireSuccess("removeFromPlaylist")

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String?,
    ) = innerTubeX.movePlaylistSong(client, playlistId, setVideoId, successorSetVideoId).requireSuccess("moveSongPlaylist")

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
    ) = innerTubeX.createPlaylist(client, title).requireSuccess("createPlaylist")

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = innerTubeX.renamePlaylist(client, playlistId, name).requireSuccess("renamePlaylist")

    suspend fun setPlaylistThumbnail(
        client: YouTubeClient,
        playlistId: String,
        image: ByteArray,
    ) = innerTubeX.setPlaylistThumbnail(client, playlistId, image).requireSuccess("setPlaylistThumbnail")

    suspend fun removePlaylistThumbnail(
        client: YouTubeClient,
        playlistId: String,
    ) = innerTubeX.removePlaylistThumbnail(client, playlistId).requireSuccess("removePlaylistThumbnail")

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = innerTubeX.deletePlaylist(client, playlistId).requireSuccess("deletePlaylist")

    suspend fun uploadSong(
        filename: String,
        data: ByteArray,
        onProgress: ((Float) -> Unit)? = null,
    ) = innerTubeX.uploadSong(filename, data, onProgress).requireSuccess("uploadSong")

    suspend fun deletePrivatelyOwnedEntity(entityId: String) =
        innerTubeX
            .deletePrivatelyOwnedEntity(YouTubeClient.WEB_REMIX, entityId)
            .requireSuccess("deletePrivatelyOwnedEntity")

    private suspend fun HttpResponse.requireSuccess(operation: String): HttpResponse {
        if (!status.isSuccess()) {
            bodyAsChannel().cancel(null)
            throw InnerTubeHttpException(operation, status)
        }
        return this
    }

    private suspend fun returnYouTubeDislike(videoId: String) =
        withRetry {
            httpClient.get("https://returnyoutubedislikeapi.com/Votes?videoId=$videoId") {
                contentType(ContentType.Application.Json)
            }
        }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (exception: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw exception
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> =
        runCatching {
            val response =
                next(
                    client = YouTubeClient.WEB,
                    videoId = videoId,
                    playlistId = null,
                    playlistSetVideoId = null,
                    index = null,
                    params = null,
                    continuation = null,
                ).body<NextResponse>()

            val baseForInfo =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find { it?.videoSecondaryInfoRenderer != null }
                    ?.videoSecondaryInfoRenderer

            val baseForTitle =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find { it?.videoPrimaryInfoRenderer != null }
                    ?.videoPrimaryInfoRenderer

            val returnYouTubeDislikeResponse =
                returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()

            MediaInfo(
                videoId = videoId,
                title = baseForTitle?.title?.runs?.firstOrNull()?.text,
                author = baseForInfo?.owner?.videoOwnerRenderer?.title?.runs?.firstOrNull()?.text,
                authorId = baseForInfo?.owner?.videoOwnerRenderer?.navigationEndpoint?.browseEndpoint?.browseId,
                authorThumbnail =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find { it.height == 48 }
                        ?.url
                        ?.replace("s48", "s960"),
                description = baseForInfo?.attributedDescription?.content,
                subscribers = baseForInfo?.owner?.videoOwnerRenderer?.subscriberCountText?.simpleText?.split(" ")?.firstOrNull(),
                uploadDate = baseForTitle?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )
        }
}
