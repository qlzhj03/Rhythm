package com.qlzhj.rhythm.feature.streaming.data.provider

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 网易云 web/客户端通道的请求加密，移植自 folia-sonnet 的 `src/nativeBridge/api/weapi.js` 与
 * `eapi.js`（后者源自 NeteaseCloudMusicApi 的 util/crypto.js）。
 *
 * - weapi：请求体 AES-128-CBC 加两层（第一层固定 preset key，第二层随机 16 位 base62 key），
 *   随机 key 逐字符反序后用无填充 RSA 加密成 encSecKey。服务网页端（music.163.com/weapi/…）。
 * - eapi：请求体按 `<path>-36cd479b6b5-<text>-36cd479b6b5-<md5>` 做 AES-128-ECB，密文十六进制
 *   大写。服务客户端端（interface.music.163.com/eapi/…）；游客注册（register/anonimous）目前
 *   只有这条路会下发 MUSIC_A / __csrf（见 folia-sonnet anonimous.js 的实测记录）。
 *
 * RSA 那步在 JVM 里就是 BigInteger.modPow。Base64 一律用 java.util.Base64（minSdk 26 起可用），
 * JVM 单测里同样成立——android.util.Base64 在单测里只会返回桩值。
 */
object NetEaseCrypto {
    private const val WEAPI_IV = "0102030405060708"
    private const val WEAPI_PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val WEAPI_BASE62 =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    /**
     * weapi 的 RSA 公钥模数（1024 bit / 256 位十六进制）。必须与官方一致：
     * 写错服务端解不开 encSecKey，回「HTTP 200 + 空 body」，界面上表现为整页读不到数据。
     */
    private val WEAPI_RSA_MODULUS =
        BigInteger(
            "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76" +
                "d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46b" +
                "ee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7",
            16,
        )
    private val WEAPI_RSA_EXPONENT = BigInteger.valueOf(65537)

    private const val EAPI_KEY = "e82ckenh8dichen8"

    /** 上游 ID_XOR_KEY_1：游客设备 id 先逐字符与它异或，再取 MD5。 */
    private const val ANON_DEVICE_ID_XOR_KEY = "3go8&\$8*3*3h0k(2)2"

    private val RANDOM = SecureRandom()

    /** @return 长度 2：[params, encSecKey]，两个都按表单值原样发送（发送方再 URL 编码）。 */
    fun weapiEncrypt(
        json: String,
        secretKey: String = randomBase62Key(),
    ): Pair<String, String> {
        val params = aesCbcEncrypt(aesCbcEncrypt(json, WEAPI_PRESET_KEY), secretKey)
        // JS 侧是把随机 key 逐字符反序后按大端字节累加成大整数。key 全是 base62 字符
        // （ASCII < 0x80），直接按无符号大端字节解释即可；用 new BigInteger(1, bytes)
        // 防止最高位是 1 时被当成补码负数。
        val reversed =
            StringBuilder(secretKey)
                .reverse()
                .toString()
                .toByteArray(StandardCharsets.US_ASCII)
        val hex = BigInteger(1, reversed).modPow(WEAPI_RSA_EXPONENT, WEAPI_RSA_MODULUS).toString(16)
        val encSecKey = StringBuilder(256)
        repeat(256 - hex.length) { encSecKey.append('0') }
        encSecKey.append(hex)
        return params to encSecKey.toString()
    }

    /**
     * eapi 加密：@return 十六进制大写密文，按表单字段 `params` 发送。
     * [path] 是 `/api/…` 形式的接口路径（签名的就是它，不是 `/eapi/…`）。
     */
    fun eapiEncrypt(
        path: String,
        json: String,
    ): String {
        val digest = md5Hex("nobody${path}use${json}md5forencrypt")
        val payload = "$path-36cd479b6b5-$json-36cd479b6b5-$digest"
        return aesEcbEncryptToHex(payload)
    }

    /** 游客注册的用户名：`base64("<deviceId> <md5-base64(xored(deviceId))>")`（52 位十六进制设备 id）。 */
    fun anonymousUsername(deviceIdHex52: String = randomDeviceIdHex52()): String {
        val xored =
            buildString {
                for (i in deviceIdHex52.indices) {
                    append((deviceIdHex52[i].code xor ANON_DEVICE_ID_XOR_KEY[i % ANON_DEVICE_ID_XOR_KEY.length].code).toChar())
                }
            }
        val encodedId = Base64.getEncoder().encodeToString(md5(xored.toByteArray(StandardCharsets.UTF_8)))
        val plain = "$deviceIdHex52 $encodedId"
        return Base64.getEncoder().encodeToString(plain.toByteArray(StandardCharsets.UTF_8))
    }

    /** 上游 generateDeviceId()：52 个十六进制字符。 */
    fun randomDeviceIdHex52(): String {
        val hex = "0123456789ABCDEF"
        return buildString { repeat(52) { append(hex[RANDOM.nextInt(hex.length)]) } }
    }

    fun md5Hex(text: String): String = md5Hex(text.toByteArray(StandardCharsets.UTF_8))

    private fun randomBase62Key(): String =
        buildString {
            repeat(16) { append(WEAPI_BASE62[RANDOM.nextInt(WEAPI_BASE62.length)]) }
        }

    private fun aesCbcEncrypt(
        text: String,
        key: String,
    ): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "AES"),
            IvParameterSpec(WEAPI_IV.toByteArray(StandardCharsets.UTF_8)),
        )
        // 与 CryptoJS 的 OpenSSL 兼容输出（base64）一致：这里就是裸密文的 base64，
        // 没有 "Salted__" 前缀——JS 侧传的是 key 字符串而非 passphrase，不走 EVP_BytesToKey。
        return Base64.getEncoder().encodeToString(cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun aesEcbEncryptToHex(payload: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(StandardCharsets.UTF_8), "AES"))
        return cipher
            .doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02X".format(it) }
    }

    private fun md5(bytes: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(bytes)

    private fun md5Hex(bytes: ByteArray): String = md5(bytes).joinToString("") { "%02x".format(it) }
}
