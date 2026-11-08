package com.qlzhj.rhythm.feature.streaming.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class NetEaseCryptoTest {
    companion object {
        private const val WEAPI_IV = "0102030405060708"
        private const val WEAPI_PRESET_KEY = "0CoJUm6Qyw8W8jud"

        // 与 folia-sonnet 的 weapi.js / eapi.js 逐字节一致的已知答案（用同一实现先算出来钉住）。
        private const val KAT_JSON = """{"csrf_token":"","key":"test-key"}"""
        private const val KAT_KEY = "Ab3xY9qLmN0pQr5Z"
        private const val KAT_PARAMS =
            "A2eGd/juGMWNucOBcpdpyYGrks4WZG4jyVceI0+XtHTao8JuiF1RicnK+qY7sedFyfRupkFHgH5BHeWzbr" +
                "QuStkcOOrPr4IyNaTCkIGRBgk="
        private const val KAT_ENC_SEC_KEY =
            "0e9f07a4412890c85e90a48b10cb4e5af224e0a6a97e670b79609b996d647489b11b38ad11932f33bc2b" +
                "37a66c4070e95b5571c0d62638d95cddb6dd9a2e8b0fb3171d415b147b62ea6de5411266ebe3c05978e" +
                "248aae88a2b63f0df3213d0a0738e1218012c4a14db3052349c67afadb10225d6f66e6746ae9d27f56b" +
                "551c49"
        private const val KAT_EAPI_PATH = "/api/register/anonimous"
        private const val KAT_EAPI_JSON = """{"username":"dGVzdA=="}"""
        private const val KAT_EAPI_PARAMS =
            "6327C0EE70AF8E1A97CC95909812274F82CC1FC318A9DE1C3BE70776DD5840E4F2D559B6EB06F84417E4" +
                "A0522EA36CBE16119F119AB66A47025CA4FA0FEEAF0E0684C67325693D0D99F12C98E7A929C6478811" +
                "DBDE5DE2AAB4C2F1D6280EAB7856AB550BB9F51195745CE46B21C53B3A"
        private const val KAT_ANON_DEVICE_ID = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123"
        private const val KAT_ANON_USERNAME =
            "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUYwMTIzNDU2Nzg5QUJDREVGMDEyMyBYa2pIc2o5dnlX" +
                "cTVRNDdCdXYyVWNnPT0="
    }

    @Test
    fun `weapi encrypt matches the reference web implementation`() {
        val (params, encSecKey) = NetEaseCrypto.weapiEncrypt(KAT_JSON, KAT_KEY)
        assertEquals(KAT_PARAMS, params)
        assertEquals(KAT_ENC_SEC_KEY, encSecKey)
    }

    @Test
    fun `weapi encrypt round trips through both AES layers`() {
        val json = """{"key":"二维码 unikey","csrf_token":"csrf-1"}"""
        val secret = "ZqR5Qp0mNlYx3bAa"
        val (params, _) = NetEaseCrypto.weapiEncrypt(json, secret)

        // preset key 在内层、随机 key 在外层：先解外层再解内层。
        val inner = decryptAesCbc(params, secret)
        assertEquals(json, decryptAesCbc(inner, WEAPI_PRESET_KEY))
    }

    @Test
    fun `weapi encrypt with a random key has the expected shape`() {
        val (params, encSecKey) = NetEaseCrypto.weapiEncrypt(KAT_JSON)
        assertEquals(256, encSecKey.length)
        assertTrue(encSecKey.all { it in "0123456789abcdef" })
        // params 是双层 AES-CBC 的 base64：密文长度 16 字节对齐。
        val decoded = Base64.getDecoder().decode(params)
        assertEquals(0, decoded.size % 16)
        assertTrue(decoded.size >= 16)
    }

    @Test
    fun `eapi encrypt matches the reference client implementation`() {
        assertEquals(KAT_EAPI_PARAMS, NetEaseCrypto.eapiEncrypt(KAT_EAPI_PATH, KAT_EAPI_JSON))
    }

    @Test
    fun `eapi encrypt signs the path it is given`() {
        val other = NetEaseCrypto.eapiEncrypt("/api/other", KAT_EAPI_JSON)
        assertTrue(other != KAT_EAPI_PARAMS)
        assertTrue(other.length % 32 == 0)
    }

    @Test
    fun `anonymous username matches the reference encoding`() {
        assertEquals(KAT_ANON_USERNAME, NetEaseCrypto.anonymousUsername(KAT_ANON_DEVICE_ID))
    }

    @Test
    fun `anonymous username decodes to deviceId plus encoded id`() {
        val username = NetEaseCrypto.anonymousUsername()
        val plain = String(Base64.getDecoder().decode(username), StandardCharsets.UTF_8)
        val (deviceId, encodedId) = plain.split(" ")
        assertEquals(52, deviceId.length)
        assertTrue(deviceId.all { it in "0123456789ABCDEF" })
        // encodedId 是 16 字节 MD5 的 base64。
        assertEquals(24, encodedId.length)
        assertTrue(encodedId.endsWith("=="))
        // 与随机 key 的 weapi 一样，同一设备两次调用给出同一用户名。
        assertEquals(username, NetEaseCrypto.anonymousUsername(deviceId))
    }

    private fun decryptAesCbc(
        base64Text: String,
        key: String,
    ): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "AES"),
            IvParameterSpec(WEAPI_IV.toByteArray(StandardCharsets.UTF_8)),
        )
        return String(cipher.doFinal(Base64.getDecoder().decode(base64Text)), StandardCharsets.UTF_8)
    }
}
