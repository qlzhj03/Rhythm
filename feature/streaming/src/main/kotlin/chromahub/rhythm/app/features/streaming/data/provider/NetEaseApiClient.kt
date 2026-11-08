package chromahub.rhythm.app.features.streaming.data.provider

import android.content.Context
import chromahub.rhythm.app.network.NetworkLog
import androidx.core.content.edit
import com.qlzhj.rhythm.core.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Signed-in NetEase account, as read from /nuser/account/get. */
data class NetEaseAccount(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
)

/** QR sign-in state machine, mirroring folia-sonnet's neteaseProvider signIn methods. */
sealed class NetEaseQrLoginState {
    /** unikey obtained; [qrContent] is what the QR image encodes. */
    data class QrReady(
        val key: String,
        val qrContent: String,
    ) : NetEaseQrLoginState()

    /** 801: code not scanned yet. */
    object Waiting : NetEaseQrLoginState()

    /** 802: scanned in the app, waiting for the user to confirm there. */
    data class Scanned(
        val nickname: String?,
        val avatarUrl: String?,
    ) : NetEaseQrLoginState()

    /** 803: confirmed — a session cookie must have arrived with the answer. */
    data class Confirmed(
        val account: NetEaseAccount?,
    ) : NetEaseQrLoginState()

    /** 800: QR code expired, a new key is needed. */
    object Expired : NetEaseQrLoginState()

    /** Any other answer (risk-control codes included) or a 803 without a session cookie. */
    data class Failed(
        val code: Int?,
        val message: String?,
    ) : NetEaseQrLoginState()
}

/**
 * NetEase Cloud Music (网易云) client: guest registration, QR sign-in and session persistence,
 * ported from folia-sonnet's native bridge (`src/nativeBridge/api/{weapi,eapi}.js`,
 * `src/nativeBridge/api/netease/anonimous.js`) and its provider login flow.
 *
 * 会话是一个 cookie 头（MUSIC_U 是登录态本身）。扫码前所有 weapi 请求都带游客 cookie——
 * weapi 的 `csrf_token` 取自 cookie 的 `__csrf`，缺了它上游一律答 code 301（folia-sonnet
 * anonimous.js 里写明的坑），所以进登录流程前先注册一次游客。
 *
 * 刻意**不发** X-Real-IP / X-Forwarded-For：folia-sonnet（UPSTREAMS.md）实测带上隐式来源 IP
 * 会让扫码确认失败。UA 用桌面 Chrome，与网页端一致。
 */
class NetEaseApiClient(
    context: Context,
    private val baseUrl: String = "https://music.163.com",
    private val eapiBaseUrl: String = "https://interface.music.163.com",
) {
    data class Session(
        val cookie: String,
        val userId: Long,
        val nickname: String,
        val avatarUrl: String,
    )

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var session: Session? = loadSession()

    private val okHttpClient =
        UserTrustManager
            .buildUserTrustingHttpClientBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    fun isConnected(): Boolean = session != null

    fun getSession(): Session? = session

    fun getUsername(): String = session?.nickname.orEmpty()

    /** 稳定的客户端设备标识（eapi header 用，随机生成后固定，频繁更换会被风控判异常）。 */
    private fun eapiDeviceId(): String {
        val stored = prefs.getString(KEY_DEVICE_ID, null)
        if (!stored.isNullOrBlank()) return stored
        val generated =
            buildString {
                val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
                repeat(16) { append(alphabet[(0 until alphabet.length).random()]) }
            }
        prefs.edit { putString(KEY_DEVICE_ID, generated) }
        return generated
    }

    private fun eapiNuid(): String {
        val stored = prefs.getString(KEY_NTES_NUID, null)
        if (!stored.isNullOrBlank()) return stored
        val generated =
            buildString {
                repeat(32) { append("0123456789abcdef"[(0 until 16).random()]) }
            }
        prefs.edit { putString(KEY_NTES_NUID, generated) }
        return generated
    }

    private fun eapiWnmcid(): String {
        val stored = prefs.getString(KEY_WNMCID, null)
        if (!stored.isNullOrBlank()) return stored
        val generated = "${randomLetters(6)}.${System.currentTimeMillis()}.01.0"
        prefs.edit { putString(KEY_WNMCID, generated) }
        return generated
    }

    private fun randomLetters(length: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        return buildString { repeat(length) { append(alphabet[(0 until alphabet.length).random()]) } }
    }

    // ---------------------------------------------------------------- cookie plumbing

    private fun parseCookieString(cookieHeader: String): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        cookieHeader.split(";").forEach { pair ->
            val separator = pair.indexOf('=')
            if (separator <= 0) return@forEach
            val name = pair.substring(0, separator).trim()
            val value = pair.substring(separator + 1).trim()
            if (name.isNotEmpty() && value.isNotEmpty()) map[name] = value
        }
        return map
    }

    /**
     * Set-Cookie 头里的 `name=value`（分号后面的属性不要）。响应头可能把多份 cookie 合并进一行，
     * 按上游 setCookieHeaders.js 的写法在「, 后面跟 name=」处切开。
     */
    private fun pairsOfSetCookies(response: Response): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        response.headers.values("Set-Cookie").forEach { raw ->
            raw.split(SET_COOKIE_MERGE_REGEX).forEach { part ->
                val head = part.substringBefore(';').trim()
                val separator = head.indexOf('=')
                if (separator <= 0) return@forEach
                val name = head.substring(0, separator).trim()
                val value = head.substring(separator + 1).trim()
                if (name.isNotEmpty() && value.isNotEmpty()) pairs.add(name to value)
            }
        }
        return pairs
    }

    private fun rememberEapiNmtid(pairs: List<Pair<String, String>>) {
        val nmtid = pairs.firstOrNull { it.first == "NMTID" }?.second ?: return
        if (nmtid.isNotBlank()) {
            prefs.edit { putString(KEY_NMTID, nmtid) }
        }
    }

    private fun csrfOf(cookieHeader: String): String = parseCookieString(cookieHeader)["__csrf"].orEmpty()

    // ---------------------------------------------------------------- transport

    /**
     * weapi POST：体里补 csrf_token，AES 双层加密后按表单发送，@return (响应 JSON, Set-Cookie 对)。
     * [path] 接受 `/api/…`、`/weapi/…` 或裸名，最终都发到 `{baseUrl}/weapi/<裸名>`。
     */
    private fun weapiPostBlocking(
        path: String,
        json: String,
        cookieHeader: String,
    ): Pair<JSONObject, List<Pair<String, String>>> {
        val apiPath = path.trimStart('/').removePrefix("weapi/").removePrefix("api/")
        val data = JSONObject(json)
        data.put("csrf_token", csrfOf(cookieHeader))
        val encrypted = NetEaseCrypto.weapiEncrypt(data.toString())
        val form = "params=${urlEncode(encrypted.first)}&encSecKey=${urlEncode(encrypted.second)}"
        val request =
            Request
                .Builder()
                .url("$baseUrl/weapi/$apiPath")
                .post(form.toRequestBody(FORM_MEDIA_TYPE))
                .header("Content-Type", FORM_CONTENT_TYPE)
                .header("User-Agent", WEAPI_UA)
                // Referer 固定为官方网页域名（与上游一致）：baseUrl 只决定请求发往哪里。
                .header("Referer", "https://music.163.com/")
                .header("Cookie", cookieHeader)
                .build()
        okHttpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val body =
                try {
                    JSONObject(text)
                } catch (_: Exception) {
                    JSONObject()
                }
            return body to pairsOfSetCookies(response)
        }
    }

    /**
     * eapi POST（官方客户端通道）：体里拼 header 块后整体 AES-ECB 加密，@return (响应 JSON, Set-Cookie 对)。
     * [apiPath] 是 `/api/…` 形式的路径；请求发往 `{eapiBaseUrl}/eapi/<去掉 /api 前缀>`。
     */
    private fun eapiPostBlocking(
        apiPath: String,
        dataJson: String,
        cookieHeader: String,
    ): Pair<JSONObject, List<Pair<String, String>>> {
        val uri = if (apiPath.startsWith("/api/")) apiPath else "/api/${apiPath.trimStart('/')}"
        val eapiPath = uri.removePrefix("/api")
        val parsed = parseCookieString(cookieHeader)
        val now = System.currentTimeMillis()
        val nuid = eapiNuid()
        val header =
            JSONObject().apply {
                put("osver", "16.2")
                put("deviceId", eapiDeviceId())
                put("appver", "9.0.90")
                put("versioncode", "140")
                put("mobilename", "")
                put("buildver", now.toString().take(10))
                put("resolution", "1920x1080")
                put("__csrf", parsed["__csrf"] ?: "")
                // 官方 iPhone 客户端的 os 就是 'iPhone OS'，写成 'ios' 与 UA 对不上。
                put("os", "iPhone OS")
                put("channel", "distribution")
                put("requestId", "${now}_${(0..9999).random().toString().padStart(4, '0')}")
                put("_ntes_nuid", nuid)
                put("_ntes_nnid", parsed["_ntes_nnid"] ?: "$nuid,$now")
                put("WNMCID", eapiWnmcid())
                put("WEVNSM", "1.0.0")
                put("__remember_me", "true")
                put("ntes_kaola_ad", "1")
                parsed["NMTID"]?.takeIf { it.isNotBlank() }?.let { put("NMTID", it) }
                parsed["MUSIC_U"]?.let { put("MUSIC_U", it) }
                parsed["MUSIC_A"]?.let { put("MUSIC_A", it) }
                // 其余会话 cookie 原样回传（官方客户端也这么干），缺一个都像「非官方客户端」。
                EAPI_PASSTHROUGH_COOKIES.forEach { name ->
                    val value = parsed[name] ?: return@forEach
                    if (value.isNotBlank() && !has(name)) put(name, value)
                }
            }
        val requestBody = JSONObject(dataJson).put("header", header)
        val params = NetEaseCrypto.eapiEncrypt(uri, requestBody.toString())
        val form = "params=${urlEncode(params)}"
        // 官方客户端的 Cookie 头与加密体里的 header 块一致；值按 encodeURIComponent 的口径编码
        // （上游 formatEapiCookieHeader 就是这么发的，MUSIC_A 这类 base64 里的 + / = 不编码会被截错）。
        val cookiePairs =
            header
                .keys()
                .asSequence()
                .filter { !header.isNull(it) && header.optString(it).isNotEmpty() }
                .map { it to header.optString(it) }
                .toList()
        val cookieForRequest =
            cookiePairs.joinToString("; ") { (name, value) ->
                "${cookieComponent(name)}=${cookieComponent(decodeCookieValue(value))}"
            }
        val request =
            Request
                .Builder()
                .url("$eapiBaseUrl/eapi$eapiPath")
                .post(form.toRequestBody(FORM_MEDIA_TYPE))
                .header("Content-Type", FORM_CONTENT_TYPE)
                .header("User-Agent", EAPI_UA)
                .header("Cookie", cookieForRequest)
                .build()
        okHttpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val body =
                try {
                    JSONObject(text)
                } catch (_: Exception) {
                    JSONObject()
                }
            return body to pairsOfSetCookies(response)
        }
    }

    private fun urlEncode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    /** encodeURIComponent 的口径：URLEncoder 把空格写成 '+'，这里还原成 '%20'。 */
    private fun cookieComponent(value: String): String = urlEncode(value).replace("+", "%20")

    /** 上游 encodeCookiePair 会先 decodeURIComponent 再编码；解不开就按原样用。 */
    private fun decodeCookieValue(value: String): String =
        try {
            java.net.URLDecoder.decode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }

    private suspend fun <T> onIo(block: suspend () -> Result<T>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                NetworkLog.w(TAG, "${e.javaClass.simpleName}: ${e.message ?: "request failed"}")
                Result.failure(e)
            }
        }

    // ---------------------------------------------------------------- guest session

    private fun guestCookie(): String = prefs.getString(KEY_GUEST_COOKIE, null).orEmpty()

    private fun isGuestCookie(cookie: String): Boolean = cookie.contains("MUSIC_A=") || cookie.contains("__csrf=")

    private fun stableIdentityCookie(): String {
        val map = parseCookieString(guestCookie())
        map.putIfAbsent("_ntes_nuid", eapiNuid())
        map.putIfAbsent("WNMCID", eapiWnmcid())
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    /**
     * 注册一个游客会话并缓存它的 cookie（MUSIC_A + __csrf）。拿不到就回空串——与上游兜底一致，
     * 不比现在更差，后续请求以匿名身份出去。eapi 在前：folia-sonnet 实测只有这条会下发
     * MUSIC_A / __csrf，weapi 留在后面是万一 eapi 哪天换路。
     */
    suspend fun ensureGuestCookie(): Result<String> =
        onIo {
            val cached = guestCookie()
            if (isGuestCookie(cached)) return@onIo Result.success(cached)

            val username = NetEaseCrypto.anonymousUsername(NetEaseCrypto.randomDeviceIdHex52())
            val (eapiBody, eapiPairs) =
                eapiPostBlocking(
                    "/api/register/anonimous",
                    "{\"username\":${jsonString(username)}}",
                    stableIdentityCookie(),
                )
            rememberEapiNmtid(eapiPairs)
            var cookie = eapiPairs.joinToString("; ") { "${it.first}=${it.second}" }
            if (!isGuestCookie(cookie)) {
                val (weapiBody, weapiPairs) = weapiPostBlocking("/register/anonimous", "{\"username\":${jsonString(username)}}", "")
                cookie = weapiPairs.joinToString("; ") { "${it.first}=${it.second}" }
                if (!isGuestCookie(cookie)) {
                    NetworkLog.i(TAG, "guest registration rejected (eapi code ${eapiBody.optInt("code")}, weapi code ${weapiBody.optInt("code")})")
                    return@onIo Result.success("")
                }
            }
            prefs.edit { putString(KEY_GUEST_COOKIE, cookie) }
            Result.success(cookie)
        }

    // ---------------------------------------------------------------- QR sign-in

    /**
     * 扫码第一步：拿 unikey。二维码内容是官方网页登录页的 codekey 链接。
     *
     * 必须走 **eapi**：同一条接口在 weapi 上会答 `code 400 参数错误`（folia-sonnet 的
     * `netease.js` 里写着「QR unikey/check must use EAPI」）；`type=3` 是 eapi/App 侧的二维码类型。
     */
    suspend fun startQrLogin(): Result<NetEaseQrLoginState.QrReady> =
        onIo {
            val cookie = ensureGuestCookie().getOrDefault("")
            val (body, _) = eapiPostBlocking("/api/login/qrcode/unikey", "{\"type\":3}", cookie)
            val unikey =
                body
                    .optString("unikey")
                    .ifBlank { body.optJSONObject("data")?.optString("unikey").orEmpty() }
                    .ifBlank { body.optString("uniKey") }
            if (unikey.isBlank()) {
                val code = body.optInt("code")
                val hint = body.optString("message").ifBlank { body.optString("msg") }
                return@onIo Result.failure(
                    IllegalStateException(
                        "NetEase gave no QR key (code $code" + (if (hint.isNotBlank()) ": $hint" else "") + ")",
                    ),
                )
            }
            Result.success(NetEaseQrLoginState.QrReady(unikey, "https://music.163.com/login?codekey=$unikey"))
        }

    /**
     * 扫码第二步：轮询。800 过期、801 等待、802 已扫码、803 确认——803 的登录态随 Set-Cookie
     * 回来（body 里有时也没有 cookie 字段），必须落到本地才算登录成功。
     *
     * 同样必须走 eapi（接口名是 `client/login`，不是网页端那条 `unikey/check`）。
     */
    suspend fun checkQrLogin(key: String): Result<NetEaseQrLoginState> =
        onIo {
            val cookie = session?.cookie ?: guestCookie()
            val (body, response) =
                eapiPostBlocking(
                    "/api/login/qrcode/client/login",
                    "{\"key\":${jsonString(key)},\"type\":3}",
                    cookie,
                )
            when (body.optInt("code")) {
                800 -> {
                    Result.success(NetEaseQrLoginState.Expired)
                }

                801 -> {
                    Result.success(NetEaseQrLoginState.Waiting)
                }

                802 -> {
                    Result.success(NetEaseQrLoginState.Scanned(body.optString("nickname"), body.optString("avatarUrl")))
                }

                803 -> {
                    // Set-Cookie 是权威来源（body 里的 cookie 字段是后端镜像出来的兜底）。
                    val answered =
                        response
                            .union(body.optString("cookie").let { parseCookieString(it).entries.map { e -> e.key to e.value } })
                            .toMap()
                    val musicU = answered["MUSIC_U"]
                    if (musicU.isNullOrBlank()) {
                        // 803 只说明「手机上点了确认」；cookie 没带回来就如实报失败，
                        // 不报「已确认」让人以为登上了（folia-sonnet 真机诊断过的坑）。
                        Result.success(
                            NetEaseQrLoginState.Failed(803, "QR confirmed but the answer carried no session cookie"),
                        )
                    } else {
                        val merged = parseCookieString(session?.cookie ?: guestCookie())
                        answered.forEach { (name, value) -> if (name != "MUSIC_A" || merged["MUSIC_A"] == null) merged[name] = value }
                        val cookieHeader = merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
                        val account = fetchAccountBlocking(cookieHeader)
                        if (account != null) {
                            persistSession(cookieHeader, account)
                        } else {
                            prefs.edit {
                                putString(KEY_COOKIE, cookieHeader)
                                remove(KEY_USER_ID)
                                remove(KEY_NICKNAME)
                                remove(KEY_AVATAR_URL)
                            }
                            session = Session(cookieHeader, 0L, "", "")
                        }
                        Result.success(NetEaseQrLoginState.Confirmed(account))
                    }
                }

                else -> {
                    Result.success(NetEaseQrLoginState.Failed(body.optInt("code"), body.optString("message")))
                }
            }
        }

    // ---------------------------------------------------------------- catalog & playback

    /** 专辑详情：专辑本身和一页歌（上游 /api/v1/album/{id}）。 */
    data class NetEaseAlbumDetail(
        val album: ProviderAlbum?,
        val songs: List<ProviderSong>,
    )

    /** eapi 读接口：payload 里补 csrf_token（上游 api() 的做法），cookie 与其它请求同一份。 */
    private fun eapiApiBlocking(
        path: String,
        data: JSONObject = JSONObject(),
        cookieHeader: String = sessionCookie(),
    ): JSONObject {
        val payload = JSONObject(data.toString())
        payload.put("csrf_token", csrfOf(cookieHeader))
        return eapiPostBlocking(path, payload.toString(), cookieHeader).first
    }

    /**
     * weapi 读接口：探索页那几条（新碟、推荐歌单、新歌）在上游就是走网页端通道的，
     * eapi 上未必有新碟这一条，照抄上游的路子。
     */
    private fun weapiApiBlocking(
        path: String,
        data: JSONObject = JSONObject(),
        cookieHeader: String = sessionCookie(),
    ): JSONObject {
        val payload = JSONObject(data.toString())
        payload.put("csrf_token", csrfOf(cookieHeader))
        return weapiPostBlocking(path, payload.toString(), cookieHeader).first
    }

    private fun sessionCookie(): String = session?.cookie ?: guestCookie()

    /** 已登录账号的 uid；游客或读不到账号时回 0。 */
    private fun currentUserIdBlocking(): Long {
        val current = session ?: return 0L
        if (current.userId > 0L) return current.userId
        val account = fetchAccountBlocking(current.cookie) ?: return 0L
        persistSession(current.cookie, account)
        return account.userId
    }

    /** 账号的歌单（「我喜欢的音乐」也在里面，specialType = 5）。 */
    suspend fun getUserPlaylists(limit: Int = 60): Result<List<ProviderPlaylist>> =
        onIo {
            val userId = currentUserIdBlocking()
            if (userId <= 0L) return@onIo Result.success(emptyList())
            val body =
                eapiApiBlocking(
                    "/api/user/playlist",
                    JSONObject().put("uid", userId).put("limit", limit).put("offset", 0),
                )
            val array = body.optJSONArray("playlist")
            NetworkLog.d(TAG, "getUserPlaylists code=${body.optInt("code")} count=${array?.length() ?: -1}")
            Result.success(playlistsOf(array))
        }

    /**
     * 歌单里的歌：上游 /api/v6/playlist/detail 一次给前 1000 首（n=1000）。
     * 超过 1000 首的歌单只取这一页——与参考实现一致，没有额外翻页。
     */
    suspend fun getPlaylistSongs(playlistId: String): Result<List<ProviderSong>> =
        onIo {
            val body =
                eapiApiBlocking(
                    "/api/v6/playlist/detail",
                    JSONObject().put("id", idValue(playlistId)).put("n", 1000).put("s", 8),
                )
            val tracks = body.optJSONObject("playlist")?.optJSONArray("tracks")
            Result.success(songsOf(tracks ?: body.optJSONArray("songs")))
        }

    /** 每日推荐：登录后才有个性化内容，游客拿到的是人人相同的一批。 */
    suspend fun getRecommendations(limit: Int = 30): Result<List<ProviderSong>> =
        onIo {
            val body = eapiApiBlocking("/api/v3/discovery/recommend/songs")
            val daily =
                body.optJSONObject("data")?.optJSONArray("dailySongs")
                    ?: body.optJSONArray("dailySongs")
            val songs = songsOf(daily).ifEmpty { songsOf(body.optJSONArray("recommend")) }
            Result.success(songs.take(limit.coerceAtLeast(1)))
        }

    /** 新碟上架（探索页的新发行）。 */
    suspend fun getNewAlbums(limit: Int = 30): Result<List<ProviderAlbum>> =
        onIo {
            val body =
                weapiApiBlocking(
                    "/album/new",
                    JSONObject()
                        .put("limit", limit)
                        .put("offset", 0)
                        .put("area", "ALL")
                        .put("total", true),
                )
            Result.success(albumsOf(body.optJSONArray("albums")))
        }

    /** 推荐歌单（上游 /playlist/list，cat=全部、order=hot）。 */
    suspend fun getFeaturedPlaylists(limit: Int = 30): Result<List<ProviderPlaylist>> =
        onIo {
            val body =
                weapiApiBlocking(
                    "/playlist/list",
                    JSONObject()
                        .put("cat", "全部")
                        .put("order", "hot")
                        .put("limit", limit)
                        .put("offset", 0)
                        .put("total", true),
                )
            Result.success(playlistsOf(body.optJSONArray("playlists")))
        }

    /** 热歌榜（官方榜单歌单）的前 [limit] 首：排行榜页的数据来源。 */
    suspend fun getTopChartSongs(limit: Int = 50): Result<List<ProviderSong>> = getPlaylistSongs(TOP_CHART_PLAYLIST_ID).map { songs -> songs.take(limit.coerceAtLeast(1)) }

    /** 专辑详情：专辑元数据 + 曲目（上游 /api/v1/album/{id}，weapi）。 */
    suspend fun getAlbum(albumId: String): Result<NetEaseAlbumDetail> =
        onIo {
            val body = weapiApiBlocking("/v1/album/${albumId.trim()}")

            Result.success(
                NetEaseAlbumDetail(
                    album = body.optJSONObject("album")?.let { albumFromJson(it) },
                    songs = songsOf(body.optJSONArray("songs")),
                ),
            )
        }

    /**
     * 歌手热门歌曲。本应用的歌手 id 是按名字生成的，所以先按名字搜到网易云歌手 id，
     * 再问 /api/v1/artist/songs（order=hot），拿不到就退到 /api/artist/top/song。
     */
    suspend fun getArtistTopSongs(
        artistName: String,
        limit: Int = 50,
    ): Result<List<ProviderSong>> =
        onIo {
            val name = artistName.trim()
            if (name.isBlank()) return@onIo Result.success(emptyList())
            val searched = artistsOf(searchBlocking(name, CLOUDSEARCH_ARTIST, 10).optJSONArray("artists"))
            val artistId =
                searched.firstOrNull { it.name.equals(name, ignoreCase = true) }?.providerId
                    ?: searched.firstOrNull()?.providerId
                    ?: return@onIo Result.success(emptyList())
            val body =
                eapiApiBlocking(
                    "/api/v1/artist/songs",
                    JSONObject()
                        .put("id", idValue(artistId))
                        .put("order", "hot")
                        .put("limit", limit)
                        .put("offset", 0),
                )
            val songs = songsOf(body.optJSONArray("songs"))
            if (songs.isNotEmpty()) return@onIo Result.success(songs.take(limit.coerceAtLeast(1)))
            val top = eapiApiBlocking("/api/artist/top/song", JSONObject().put("id", idValue(artistId)))
            Result.success(songsOf(top.optJSONArray("songs")).take(limit.coerceAtLeast(1)))
        }

    suspend fun searchSongs(
        query: String,
        limit: Int = 30,
    ): Result<List<ProviderSong>> =
        onIo {
            Result.success(songsOf(searchBlocking(query, CLOUDSEARCH_SONG, limit).optJSONArray("songs")))
        }

    suspend fun searchAlbums(
        query: String,
        limit: Int = 20,
    ): Result<List<ProviderAlbum>> =
        onIo {
            Result.success(albumsOf(searchBlocking(query, CLOUDSEARCH_ALBUM, limit).optJSONArray("albums")))
        }

    suspend fun searchArtists(
        query: String,
        limit: Int = 20,
    ): Result<List<ProviderArtist>> =
        onIo {
            Result.success(artistsOf(searchBlocking(query, CLOUDSEARCH_ARTIST, limit).optJSONArray("artists")))
        }

    suspend fun searchPlaylists(
        query: String,
        limit: Int = 20,
    ): Result<List<ProviderPlaylist>> =
        onIo {
            Result.success(playlistsOf(searchBlocking(query, CLOUDSEARCH_PLAYLIST, limit).optJSONArray("playlists")))
        }

    /**
     * 播流地址：/api/song/enhance/player/url/v1（eapi）。[level] 传网易云的档位
     * （standard / exhigh / lossless / hires / jymaster）。
     *
     * 拿不到 url 就降一档重问（请求的档位没有版权时上游回 url=null，不是报错）；仍拿不到
     * 回 null。只给 30 秒试听的（freeTrialInfo 非空）如实当拿不到：本应用的播放器没有
     * 「试听片段」提示，返回它只会让用户以为歌放到一半坏了。
     */
    suspend fun getSongUrl(
        songId: String,
        level: String = "exhigh",
    ): Result<String?> =
        onIo {
            val cookie = sessionCookie()
            val requested = level.trim().ifBlank { "exhigh" }
            val resolved =
                requestSongUrlBlocking(songId, requested, cookie)
                    ?: if (requested != "standard") requestSongUrlBlocking(songId, "standard", cookie) else null
            Result.success(resolved)
        }

    private fun requestSongUrlBlocking(
        songId: String,
        level: String,
        cookieHeader: String,
    ): String? {
        val body =
            eapiApiBlocking(
                "/api/song/enhance/player/url/v1",
                JSONObject().put("ids", "[${songId.trim()}]").put("level", level).put("encodeType", "flac"),
                cookieHeader,
            )
        val entry = body.optJSONArray("data")?.optJSONObject(0) ?: return null
        val url = entry.optString("url")
        if (url.isBlank()) return null
        if (entry.opt("freeTrialInfo") is JSONObject) {
            NetworkLog.i(TAG, "song ${songId.trim()} only offers a 30s trial at $level")
            return null
        }
        return url
    }

    /** 歌词：/api/song/lyric（lv=-1 原文、tv=-1 译文），译文按时间戳并进纯文本。 */
    suspend fun getLyrics(songId: String): Result<LyricsData?> =
        onIo {
            val body =
                eapiApiBlocking(
                    "/api/song/lyric",
                    JSONObject()
                        .put("id", idValue(songId))
                        .put("lv", -1)
                        .put("tv", -1)
                        .put("kv", -1),
                )
            val lrc = body.optJSONObject("lrc")?.optString("lyric").orEmpty()
            val translation = body.optJSONObject("tlyric")?.optString("lyric").orEmpty()
            if (lrc.isBlank() && translation.isBlank()) {
                Result.success(null)
            } else {
                Result.success(
                    LyricsData(
                        plainLyrics = plainLyricsOf(lrc, translation),
                        syncedLyrics = lrc.takeIf { it.isNotBlank() },
                        source = "NetEase",
                    ),
                )
            }
        }

    // ---------------------------------------------------------------- catalog mapping

    private fun searchBlocking(
        query: String,
        type: Int,
        limit: Int,
    ): JSONObject {
        val body =
            eapiApiBlocking(
                "/api/cloudsearch/pc",
                JSONObject()
                    .put("s", query.trim())
                    .put("type", type)
                    .put("limit", limit.coerceIn(1, 100))
                    .put("offset", 0)
                    .put("total", true),
            )
        return body.optJSONObject("result") ?: body
    }

    /** 数字 id 按数值发（上游 JS 里也是 number）；非数字原样发。 */
    private fun idValue(raw: String): Any {
        val trimmed = raw.trim()
        return trimmed.toLongOrNull() ?: trimmed
    }

    private fun songsOf(array: JSONArray?): List<ProviderSong> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let { songFromJson(it) } }
    }

    private fun playlistsOf(array: JSONArray?): List<ProviderPlaylist> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { playlistFromJson(it) }
        }
    }

    private fun albumsOf(array: JSONArray?): List<ProviderAlbum> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let { albumFromJson(it) } }
    }

    private fun artistsOf(array: JSONArray?): List<ProviderArtist> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { artistFromJson(it) }
        }
    }

    /** 歌曲字段名有两种写法：`/v3/song/detail` 与搜索用 `al/ar`，其余接口用 `album/artists`。 */
    private fun songFromJson(json: JSONObject): ProviderSong? {
        val providerId =
            json.optLong("id", 0L).takeIf { it > 0L }?.toString()
                ?: json.optString("id").takeIf { it.isNotBlank() }
                ?: return null
        val title = json.optString("name").ifBlank { json.optString("title") }
        if (title.isBlank()) return null
        val artistNames = artistNamesOf(json)
        val album = json.optJSONObject("al") ?: json.optJSONObject("album")
        val duration = json.optLong("dt", 0L).takeIf { it > 0L } ?: json.optLong("duration", 0L)
        return ProviderSong(
            providerId = providerId,
            title = title,
            artist = artistNames.joinToString(" / "),
            album = album?.optString("name").orEmpty(),
            durationMs = duration,
            artworkUrl = album?.optString("picUrl")?.takeIf { it.isNotBlank() },
            albumId = album?.optLong("id", 0L)?.takeIf { it > 0L }?.toString(),
            albumArtist = artistNames.firstOrNull(),
            trackNumber = json.optInt("no", 0).takeIf { it > 0 },
            year = album?.optLong("publishTime", 0L)?.takeIf { it > 0L }?.let { yearOf(it) },
        )
    }

    private fun artistNamesOf(json: JSONObject): List<String> {
        val array = json.optJSONArray("ar") ?: json.optJSONArray("artists") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
    }

    private fun playlistFromJson(json: JSONObject): ProviderPlaylist? {
        val providerId =
            json.optLong("id", 0L).takeIf { it > 0L }?.toString()
                ?: json.optString("id").takeIf { it.isNotBlank() }
                ?: return null
        val name = json.optString("name").ifBlank { json.optString("title") }
        if (name.isBlank()) return null
        return ProviderPlaylist(
            providerId = providerId,
            name = name,
            description = json.optString("description").takeIf { it.isNotBlank() },
            artworkUrl =
                json
                    .optString("coverImgUrl")
                    .ifBlank { json.optString("picUrl") }
                    .takeIf { it.isNotBlank() },
            songCount = json.optInt("trackCount", 0).takeIf { it > 0 } ?: json.optInt("songCount", 0),
            owner = json.optJSONObject("creator")?.optString("nickname")?.takeIf { it.isNotBlank() },
            isPublic = json.optInt("privacy", 0) == 0,
        )
    }

    private fun albumFromJson(json: JSONObject): ProviderAlbum? {
        val providerId =
            json.optLong("id", 0L).takeIf { it > 0L }?.toString()
                ?: json.optString("id").takeIf { it.isNotBlank() }
                ?: return null
        val title = json.optString("name").ifBlank { json.optString("title") }
        if (title.isBlank()) return null
        val artist =
            json.optJSONObject("artist")?.optString("name")?.takeIf { it.isNotBlank() }
                ?: artistNamesOf(json).joinToString(" / ")
        return ProviderAlbum(
            providerId = providerId,
            title = title,
            artist = artist,
            artworkUrl =
                json
                    .optString("picUrl")
                    .ifBlank { json.optString("blurPicUrl") }
                    .takeIf { it.isNotBlank() },
            songCount = json.optInt("size", 0).takeIf { it > 0 } ?: json.optInt("songCount", 0),
            year = json.optLong("publishTime", 0L).takeIf { it > 0L }?.let { yearOf(it) },
        )
    }

    private fun artistFromJson(json: JSONObject): ProviderArtist? {
        val providerId =
            json.optLong("id", 0L).takeIf { it > 0L }?.toString()
                ?: json.optString("id").takeIf { it.isNotBlank() }
                ?: return null
        val name = json.optString("name").ifBlank { json.optString("artistName") }
        if (name.isBlank()) return null
        return ProviderArtist(
            providerId = providerId,
            name = name,
            artworkUrl =
                json
                    .optString("picUrl")
                    .ifBlank { json.optString("img1v1Url") }
                    .takeIf { it.isNotBlank() },
            songCount = json.optInt("musicSize", 0).takeIf { it > 0 } ?: 0,
            albumCount = json.optInt("albumSize", 0).takeIf { it > 0 } ?: 0,
        )
    }

    private fun yearOf(epochMillis: Long): Int? =
        runCatching {
            java.time.Instant
                .ofEpochMilli(epochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .year
        }.getOrNull()

    /**
     * 带译文的纯文本歌词：逐行处理后把同一时间戳的译文接在原行下面。
     * 同步歌词保持上游原文（带时间戳），由播放器自己解析。
     */
    private fun plainLyricsOf(
        lrc: String,
        translation: String,
    ): String? {
        val translated =
            LRC_LINE_REGEX
                .findAll(translation)
                .associate { match -> match.groupValues[1] to match.groupValues[2].trim() }
        val lines =
            LRC_LINE_REGEX
                .findAll(lrc)
                .map { match ->
                    val text = match.groupValues[2].trim()
                    val extra = if (text.isNotBlank()) translated[match.groupValues[1]] else null
                    if (extra.isNullOrBlank()) text else "$text\n$extra"
                }.filter { it.isNotBlank() }
                .toList()
        return lines.joinToString("\n").takeIf { it.isNotBlank() }
    }

    // ---------------------------------------------------------------- account

    /** @return 登录账号；未登录（无 profile）回 null。 */
    suspend fun getAccount(): Result<NetEaseAccount?> =
        onIo {
            val cookie = session?.cookie ?: return@onIo Result.success(null)
            val account = fetchAccountBlocking(cookie) ?: return@onIo Result.success(null)
            Result.success(account)
        }

    suspend fun ping(): Result<Boolean> =
        onIo {
            val cookie = session?.cookie ?: return@onIo Result.failure(IllegalStateException("NetEase service is not connected"))
            Result.success(fetchAccountBlocking(cookie) != null)
        }

    /** 用给定的 cookie 问一次 /nuser/account/get，读出账号；回答里没有 profile 视为未登录。 */
    private fun fetchAccountBlocking(cookieHeader: String): NetEaseAccount? {
        val (body, _) = weapiPostBlocking("/nuser/account/get", "{}", cookieHeader)
        if (body.optInt("code") != 200) return null
        val profile = body.optJSONObject("profile") ?: return null
        val account = body.optJSONObject("account")
        return NetEaseAccount(
            userId = account?.optLong("id", 0L) ?: 0L,
            nickname = profile.optString("nickname"),
            avatarUrl = profile.optString("avatarUrl"),
        )
    }

    suspend fun logout() {
        val current = session
        session = null
        prefs.edit {
            remove(KEY_COOKIE)
            remove(KEY_USER_ID)
            remove(KEY_NICKNAME)
            remove(KEY_AVATAR_URL)
        }
        if (current != null) {
            // 尽力通知服务端作废会话：读不到结果也照常放行，绝不把登出卡在等待上
            // （时间上限同上游 logout.js 的 CLEANUP_TIMEOUT_MS）。
            withContext(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(2500L) {
                        weapiPostBlocking("/logout", "{}", current.cookie)
                    }
                } catch (e: Exception) {
                    NetworkLog.w(TAG, "logout call failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun persistSession(
        cookieHeader: String,
        account: NetEaseAccount,
    ) {
        session = Session(cookieHeader, account.userId, account.nickname, account.avatarUrl)
        prefs.edit {
            putString(KEY_COOKIE, cookieHeader)
            putLong(KEY_USER_ID, account.userId)
            putString(KEY_NICKNAME, account.nickname)
            putString(KEY_AVATAR_URL, account.avatarUrl)
        }
    }

    private fun loadSession(): Session? {
        val cookie = prefs.getString(KEY_COOKIE, null) ?: return null
        if (cookie.isBlank() || !cookie.contains("MUSIC_U=")) return null
        return Session(
            cookie = cookie,
            userId = prefs.getLong(KEY_USER_ID, 0L),
            nickname = prefs.getString(KEY_NICKNAME, null).orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null).orEmpty(),
        )
    }

    private fun jsonString(value: String): String = JSONObject.quote(value)

    companion object {
        private const val TAG = "NetEaseApiClient"
        private const val PREFS_NAME = "streaming_netease_credentials"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_GUEST_COOKIE = "guest_cookie"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_NTES_NUID = "ntes_nuid"
        private const val KEY_WNMCID = "wnmcid"
        private const val KEY_NMTID = "nmtid"

        /** 官方「热歌榜」歌单，排行榜页用它取歌。 */
        private const val TOP_CHART_PLAYLIST_ID = "3778678"

        /** /api/cloudsearch/pc 的 type：1 歌曲、10 专辑、100 歌手、1000 歌单。 */
        private const val CLOUDSEARCH_SONG = 1
        private const val CLOUDSEARCH_ALBUM = 10
        private const val CLOUDSEARCH_ARTIST = 100
        private const val CLOUDSEARCH_PLAYLIST = 1000

        /** LRC 行：`[mm:ss.SSS]正文`（逐行匹配，所以要 MULTILINE）。 */
        private val LRC_LINE_REGEX =
            Regex("""^\[(\d{2}:\d{2}(?:[.:]\d{1,3})?)](.*)$""", RegexOption.MULTILINE)

        private const val FORM_CONTENT_TYPE = "application/x-www-form-urlencoded"
        private val FORM_MEDIA_TYPE = FORM_CONTENT_TYPE.toMediaType()

        // 与网页端一致的桌面 Chrome UA；不显式伪装来源 IP（见类注释）。
        private const val WEAPI_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"

        /** 官方 iPhone 客户端 UA（eapi 通道）。 */
        private const val EAPI_UA = "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"

        /** eapi 的 Cookie 头顺带回传的会话 cookie（与上游 EAPI_COOKIE_KEYS 的有效子集一致）。 */
        private val EAPI_PASSTHROUGH_COOKIES =
            listOf("MUSIC_R_U", "deviceId", "sDeviceId", "WM_TID", "WM_NI", "WM_NIKE")

        private val SET_COOKIE_MERGE_REGEX = Regex(",(?=\\s*[^;]+=)")
    }
}
