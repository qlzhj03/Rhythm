package chromahub.rhythm.app.features.streaming.data.provider

import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Headers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

/**
 * 扫码登录的端到端行为，答的是假网易云（MockWebServer）。加密本身由 NetEaseCryptoTest 钉住，
 * 这里只验证流程：游客注册 → 取 unikey → 轮询 → 803 落 cookie → 读账号。
 */
class NetEaseApiClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: NetEaseApiClient
    private lateinit var baseUrl: String

    /** path → canned response；未登记的 path 回 404。 */
    private val responses = ConcurrentHashMap<String, MockResponse>()
    private val requests = ConcurrentHashMap<String, RecordedRequest>()

    /** client 落盘 SharedPreferences 的真实存储（MockContext 始终回同一份 map 的视图）。 */
    private val sharedPrefsStorage = ConcurrentHashMap<String, Any>()

    @Before
    fun setUp() {
        sharedPrefsStorage.clear()
        responses.clear()
        requests.clear()
        mockServer = MockWebServer()
        mockServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests[request.url.encodedPath] = request
                    return responses[request.url.encodedPath]
                        ?: MockResponse.Builder().code(404).build()
                }
            }
        mockServer.start()
        baseUrl = mockServer.url("/").toString().trimEnd('/')
        client = NetEaseApiClient(MockContext(sharedPrefsStorage), baseUrl = baseUrl, eapiBaseUrl = baseUrl)

        // 游客注册：eapi 先拒（不下发 cookie），weapi 兜底成功——按 folia-sonnet 实测的顺序。
        responses["/eapi/register/anonimous"] =
            MockResponse
                .Builder()
                .code(200)
                .body("""{"code":200}""")
                .build()
        responses["/weapi/register/anonimous"] =
            MockResponse
                .Builder()
                .code(200)
                .headers(
                    Headers.headersOf(
                        "Set-Cookie",
                        "MUSIC_A=guest-a; Path=/",
                        "Set-Cookie",
                        "__csrf=csrf-guest; Path=/",
                    ),
                ).body("""{"code":200}""")
                .build()
    }

    @After
    fun tearDown() {
        mockServer.close()
    }

    @Test
    fun `startQrLogin takes the unikey over eapi with the guest cookie`() =
        runBlocking {
            responses["/eapi/login/qrcode/unikey"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"code":200,"data":{"code":200,"unikey":"unikey-1"}}""")
                    .build()

            val result = client.startQrLogin()

            val ready = result.getOrNull()
            assertTrue(result.isSuccess)
            assertNotNull(ready)
            assertEquals("unikey-1", ready!!.key)
            assertEquals("https://music.163.com/login?codekey=unikey-1", ready.qrContent)
            assertFalse(client.isConnected())

            // 游客注册先走 eapi 被拒，再走 weapi 拿到 cookie；unikey 请求带上它。
            val eapiRegister = requests["/eapi/register/anonimous"]
            assertNotNull(eapiRegister)
            assertEquals(
                "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)",
                eapiRegister!!.headers["User-Agent"],
            )
            assertTrue(eapiRegister.body!!.utf8().contains("params="))
            assertTrue(eapiRegister.url.encodedPath.endsWith("/eapi/register/anonimous"))

            // unikey 必须走 eapi：同一条接口在 weapi 上答 code 400（folia-sonnet 实测）。
            val unikeyRequest = requests["/eapi/login/qrcode/unikey"]
            assertNotNull(unikeyRequest)
            assertEquals(
                "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)",
                unikeyRequest!!.headers["User-Agent"],
            )
            val cookieHeader = unikeyRequest.headers["Cookie"].orEmpty()
            assertTrue("unikey 请求必须带游客 cookie", cookieHeader.contains("MUSIC_A=guest-a"))

            val form = unikeyRequest.body!!.utf8()
            assertTrue("eapi 请求体只有 params", form.contains("params="))
            assertFalse("eapi 不走 weapi 的 encSecKey", form.contains("encSecKey="))
            assertNull("eapi 通道不带网页端 Referer", unikeyRequest.headers["Referer"])
            // 关键红线：绝不带隐式来源 IP 头，否则扫码确认会失败（folia-sonnet UPSTREAMS 实测）。
            assertNull(unikeyRequest.headers["X-Real-IP"])
            assertNull(unikeyRequest.headers["X-Forwarded-For"])

            val guest = sharedPrefsStorage["guest_cookie"] as? String
            assertTrue(guest.orEmpty().contains("MUSIC_A=guest-a"))
        }

    @Test
    fun `checkQrLogin maps waiting scanned and expired codes`() =
        runBlocking {
            responses["/eapi/login/qrcode/client/login"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"code":801,"message":"等待扫码"}""")
                    .build()
            assertEquals(NetEaseQrLoginState.Waiting, client.checkQrLogin("unikey-1").getOrNull())

            responses["/eapi/login/qrcode/client/login"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"code":802,"nickname":"扫码人","avatarUrl":"https://p1.music.126.net/x.jpg"}""")
                    .build()
            val scanned = client.checkQrLogin("unikey-1").getOrNull()
            assertTrue(scanned is NetEaseQrLoginState.Scanned)
            assertEquals("扫码人", (scanned as NetEaseQrLoginState.Scanned).nickname)

            responses["/eapi/login/qrcode/client/login"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"code":800,"message":"二维码已过期"}""")
                    .build()
            assertEquals(NetEaseQrLoginState.Expired, client.checkQrLogin("unikey-1").getOrNull())
            assertFalse(client.isConnected())
        }

    @Test
    fun `confirmed 803 persists the session cookie and reads the account`() =
        runBlocking {
            responses["/eapi/login/qrcode/client/login"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .headers(
                        Headers.headersOf(
                            "Set-Cookie",
                            "MUSIC_U=qr-session; Path=/",
                            "Set-Cookie",
                            "__csrf=csrf-login; Path=/",
                        ),
                    ).body("""{"code":803}""")
                    .build()
            responses["/weapi/nuser/account/get"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"account":{"id":42},"profile":{"nickname":"Tester",""" +
                            """"avatarUrl":"https://p1.music.126.net/a.jpg"}}""",
                    ).build()

            val confirmed = client.checkQrLogin("unikey-1").getOrNull()

            assertTrue(confirmed is NetEaseQrLoginState.Confirmed)
            val account = (confirmed as NetEaseQrLoginState.Confirmed).account
            assertNotNull(account)
            assertEquals(42L, account!!.userId)
            assertEquals("Tester", account.nickname)
            assertTrue(client.isConnected())
            assertEquals("Tester", client.getUsername())

            // 账号请求带着刚落盘的登录 cookie。
            val accountRequest = requests["/weapi/nuser/account/get"]
            assertNotNull(accountRequest)
            val cookieHeader = accountRequest!!.headers["Cookie"].orEmpty()
            assertTrue(cookieHeader.contains("MUSIC_U=qr-session"))
            assertTrue(cookieHeader.contains("__csrf=csrf-login"))

            // 登录 cookie 落盘（本测试没有游客注册，所以没有 MUSIC_A 可合并）。
            val stored = sharedPrefsStorage["cookie"] as? String
            assertTrue(stored.orEmpty().contains("MUSIC_U=qr-session"))
            assertTrue(stored.orEmpty().contains("__csrf=csrf-login"))
        }

    @Test
    fun `803 without a session cookie reports failure instead of success`() =
        runBlocking {
            responses["/eapi/login/qrcode/client/login"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"code":803}""")
                    .build()

            val state = client.checkQrLogin("unikey-1").getOrNull()

            assertTrue(state is NetEaseQrLoginState.Failed)
            assertEquals(803, (state as NetEaseQrLoginState.Failed).code)
            assertFalse(client.isConnected())
        }

    @Test
    fun `ping fails when not signed in and verifies a stored session`() =
        runBlocking {
            assertTrue(client.ping().isFailure)

            // 预置一份已落盘的会话（模拟扫码完成后的状态），账号接口答 200 → ping 通过。
            sharedPrefsStorage["cookie"] = "MUSIC_U=qr-session; MUSIC_A=guest-a; __csrf=csrf-login"
            val preloaded = NetEaseApiClient(MockContext(sharedPrefsStorage), baseUrl = baseUrl, eapiBaseUrl = baseUrl)
            responses["/weapi/nuser/account/get"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"account":{"id":42},"profile":{"nickname":"Tester",""" +
                            """"avatarUrl":"https://p1.music.126.net/a.jpg"}}""",
                    ).build()

            val result = preloaded.ping()
            assertTrue(result.isSuccess)
            assertTrue(result.getOrDefault(false))

            // 会话被服务端吊销（无 profile）→ ping 回 false 而不是异常。
            responses["/weapi/nuser/account/get"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"code":200}""")
                    .build()
            val revoked = preloaded.ping()
            assertTrue(revoked.isSuccess)
            assertFalse(revoked.getOrDefault(true))
        }

    @Test
    fun `getUserPlaylists reads the account playlists over eapi`() =
        runBlocking {
            sharedPrefsStorage["cookie"] = "MUSIC_U=qr-session; __csrf=csrf-login"
            sharedPrefsStorage["user_id"] = 42L
            val signedIn = NetEaseApiClient(MockContext(sharedPrefsStorage), baseUrl = baseUrl, eapiBaseUrl = baseUrl)
            responses["/eapi/user/playlist"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"playlist":[""" +
                            """{"id":501,"name":"我喜欢的音乐","coverImgUrl":"https://p1.music.126.net/c.jpg",""" +
                            """"trackCount":12,"creator":{"nickname":"Tester"}},""" +
                            """{"id":"502","name":"Road Trip","trackCount":3}]}""",
                    ).build()

            val playlists = signedIn.getUserPlaylists().getOrThrow()

            assertEquals(2, playlists.size)
            assertEquals("501", playlists[0].providerId)
            assertEquals("我喜欢的音乐", playlists[0].name)
            assertEquals("Tester", playlists[0].owner)
            assertEquals(12, playlists[0].songCount)
            assertEquals("https://p1.music.126.net/c.jpg", playlists[0].artworkUrl)
            // 字符串形式的 id 也要认（上游偶尔给 "502" 而不是 502）。
            assertEquals("502", playlists[1].providerId)
            assertEquals("Road Trip", playlists[1].name)

            val request = requests["/eapi/user/playlist"]
            assertNotNull(request)
            assertTrue(request!!.body!!.utf8().contains("params="))
        }

    @Test
    fun `getPlaylistSongs maps the playlist tracks`() =
        runBlocking {
            responses["/eapi/v6/playlist/detail"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"playlist":{"tracks":[""" +
                            """{"id":11,"name":"Song A","ar":[{"name":"Artist A"},{"name":"Artist B"}],""" +
                            """"al":{"id":99,"name":"Album A","picUrl":"https://p1.music.126.net/x.jpg",""" +
                            """"publishTime":1577836800000},"dt":215000,"no":2},""" +
                            """{"id":12,"name":"Song B","artists":[{"name":"Artist C"}],""" +
                            """"album":{"id":98,"name":"Album B"},"duration":180000}]}}""",
                    ).build()

            val songs = client.getPlaylistSongs("501").getOrThrow()

            assertEquals(2, songs.size)
            assertEquals("11", songs[0].providerId)
            assertEquals("Song A", songs[0].title)
            assertEquals("Artist A / Artist B", songs[0].artist)
            assertEquals("Album A", songs[0].album)
            assertEquals("99", songs[0].albumId)
            assertEquals(215_000L, songs[0].durationMs)
            assertEquals(2, songs[0].trackNumber)
            assertEquals(2020, songs[0].year)
            // 老字段名（artists/album/duration）同样要认。
            assertEquals("Artist C", songs[1].artist)
            assertEquals("Album B", songs[1].album)
            assertEquals(180_000L, songs[1].durationMs)
        }

    @Test
    fun `getAlbum maps the album and its songs over weapi`() =
        runBlocking {
            responses["/weapi/v1/album/99"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"album":{"id":99,"name":"Album A","picUrl":"https://p1.music.126.net/a.jpg",""" +
                            """"artist":{"name":"Artist A"},"size":1,"publishTime":1577836800000},""" +
                            """"songs":[{"id":11,"name":"Song A","ar":[{"name":"Artist A"}],""" +
                            """"al":{"id":99,"name":"Album A"},"dt":215000}]}""",
                    ).build()

            val detail = client.getAlbum("99").getOrThrow()

            assertEquals("Album A", detail.album?.title)
            assertEquals("Artist A", detail.album?.artist)
            assertEquals(1, detail.album?.songCount)
            assertEquals(2020, detail.album?.year)
            assertEquals(1, detail.songs.size)
            assertEquals("Song A", detail.songs[0].title)

            // 专辑详情走的是网页端通道，必须带 Referer 与加密表单。
            val request = requests["/weapi/v1/album/99"]
            assertNotNull(request)
            assertEquals("https://music.163.com/", request!!.headers["Referer"])
            assertTrue(request.body!!.utf8().contains("encSecKey="))
        }

    @Test
    fun `getSongUrl asks player url v1 and drops 30s trials`() =
        runBlocking {
            responses["/eapi/song/enhance/player/url/v1"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"data":[{"id":11,"url":"https://m7.music.126.net/a.mp3",""" +
                            """"br":320000,"level":"exhigh"}]}""",
                    ).build()

            assertEquals(
                "https://m7.music.126.net/a.mp3",
                client.getSongUrl("11", "exhigh").getOrNull(),
            )

            // 只剩免费试听（freeTrialInfo）时如实当拿不到，播放器不会突然在 30 秒处断掉。
            responses["/eapi/song/enhance/player/url/v1"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"data":[{"id":11,"url":"https://m7.music.126.net/trial.mp3",""" +
                            """"br":128000,"freeTrialInfo":{"start":0,"end":30}}]}""",
                    ).build()
            assertNull(client.getSongUrl("11", "exhigh").getOrNull())
        }

    @Test
    fun `searchSongs maps cloudsearch results`() =
        runBlocking {
            responses["/eapi/cloudsearch/pc"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"result":{"songs":[""" +
                            """{"id":21,"name":"Found","ar":[{"name":"Singer"}],""" +
                            """"al":{"id":7,"name":"Record","picUrl":"https://p1.music.126.net/r.jpg"},"dt":123000}]}}""",
                    ).build()

            val songs = client.searchSongs("found").getOrThrow()

            assertEquals(1, songs.size)
            assertEquals("21", songs[0].providerId)
            assertEquals("Found", songs[0].title)
            assertEquals("Singer", songs[0].artist)
            assertEquals("Record", songs[0].album)
            assertEquals(123_000L, songs[0].durationMs)
        }

    @Test
    fun `getLyrics returns synced lines and merges the translation into plain text`() =
        runBlocking {
            responses["/eapi/song/lyric"] =
                MockResponse
                    .Builder()
                    .code(200)
                    .body(
                        """{"code":200,"lrc":{"lyric":"[00:01.00]Hello\n[00:02.50]World"},""" +
                            """"tlyric":{"lyric":"[00:01.00]你好"}}""",
                    ).build()

            val lyrics = client.getLyrics("11").getOrThrow()

            assertNotNull(lyrics)
            assertEquals("[00:01.00]Hello\n[00:02.50]World", lyrics?.syncedLyrics)
            assertEquals("Hello\n你好\nWorld", lyrics?.plainLyrics)
            assertEquals("NetEase", lyrics?.source)
        }

    private class MockContext(
        private val values: Map<String, Any> = emptyMap(),
    ) : ContextWrapper(null) {
        override fun getSharedPreferences(
            name: String?,
            mode: Int,
        ): SharedPreferences = MockPreferences(values)
    }

    private class MockPreferences(
        initial: Map<String, Any>,
    ) : SharedPreferences {
        // 传入的已是 ConcurrentHashMap 时直接用同一份 map，写入对断言可见（不是拷贝）。
        private val storage: MutableMap<String, Any> =
            if (initial is ConcurrentHashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                (initial as MutableMap<String, Any>)
            } else {
                ConcurrentHashMap(initial)
            }

        override fun getAll(): Map<String, *> = storage

        override fun getString(
            key: String,
            defValue: String?,
        ) = storage[key] as? String ?: defValue

        override fun getStringSet(
            key: String,
            defValues: Set<String>?,
        ) =
            @Suppress("UNCHECKED_CAST")
            (storage[key] as? Set<String> ?: defValues)

        override fun getInt(
            key: String,
            defValue: Int,
        ) = storage[key] as? Int ?: defValue

        override fun getLong(
            key: String,
            defValue: Long,
        ) = storage[key] as? Long ?: defValue

        override fun getFloat(
            key: String,
            defValue: Float,
        ) = storage[key] as? Float ?: defValue

        override fun getBoolean(
            key: String,
            defValue: Boolean,
        ) = storage[key] as? Boolean ?: defValue

        override fun contains(key: String) = storage.containsKey(key)

        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        override fun edit(): SharedPreferences.Editor = MockEditor(storage)
    }

    private class MockEditor(
        private val storage: MutableMap<String, Any>,
    ) : SharedPreferences.Editor {
        private val modifications = HashMap<String, Any?>()
        private var clearRequested = false

        override fun putString(
            key: String,
            value: String?,
        ): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>?,
        ): SharedPreferences.Editor {
            modifications[key] = values
            return this
        }

        override fun putInt(
            key: String,
            value: Int,
        ): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putLong(
            key: String,
            value: Long,
        ): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putFloat(
            key: String,
            value: Float,
        ): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putBoolean(
            key: String,
            value: Boolean,
        ): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            modifications[key] = this
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) storage.clear()
            modifications.forEach { (k, v) ->
                if (v === this) {
                    storage.remove(k)
                } else if (v != null) {
                    storage[k] = v
                } else {
                    storage.remove(k)
                }
            }
        }
    }
}
