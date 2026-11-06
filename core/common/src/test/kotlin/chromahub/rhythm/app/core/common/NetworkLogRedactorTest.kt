package chromahub.rhythm.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogRedactorTest {

    @Test
    fun testSubsonicCredentialsAreRedacted() {
        val url = "https://demo.navidrome.org/rest/stream?id=abc&u=alice&p=enc:deadbeef&t=md5hash&s=salt&v=1.16.1&c=Rhythm&f=json"
        val redacted = redactUrl(url)

        assertTrue(redacted.contains("id=abc"))
        assertTrue(redacted.contains("v=1.16.1"))
        assertTrue(redacted.contains("c=Rhythm"))
        assertTrue(redacted.contains("u=$REDACTED_PLACEHOLDER"))
        assertTrue(redacted.contains("p=$REDACTED_PLACEHOLDER"))
        assertTrue(redacted.contains("t=$REDACTED_PLACEHOLDER"))
        assertTrue(redacted.contains("s=$REDACTED_PLACEHOLDER"))
        assertFalse(redacted.contains("deadbeef"))
        assertFalse(redacted.contains("md5hash"))
        assertFalse(redacted.contains("salt"))
    }

    @Test
    fun testJellyfinApiKeyIsCaseInsensitive() {
        val redacted = redactUrl("https://jf.example.com/Items/123/Download?ApiKey=secret1&api_key=secret2")

        assertTrue(redacted.contains("ApiKey=$REDACTED_PLACEHOLDER"))
        assertTrue(redacted.contains("api_key=$REDACTED_PLACEHOLDER"))
        assertFalse(redacted.contains("secret1"))
        assertFalse(redacted.contains("secret2"))
    }

    @Test
    fun testNonSensitiveUrlIsUnchanged() {
        val url = "https://api.deezer.com/search?q=hello&limit=5"
        assertEquals(url, redactUrl(url))
    }

    @Test
    fun testFragmentIsPreserved() {
        assertEquals(
            "https://host/rest/ping?token=$REDACTED_PLACEHOLDER#section",
            redactUrl("https://host/rest/ping?token=abc123#section")
        )
    }

    @Test
    fun testUrlWithoutQueryIsUnchanged() {
        assertEquals("https://host/path", redactUrl("https://host/path"))
        assertEquals("https://host/path?", redactUrl("https://host/path?"))
    }

    @Test
    fun testInterceptorRequestLineIsSanitized() {
        val sanitized = sanitizeLogMessage("--> GET https://host/rest/ping?u=alice&p=enc:secret&v=1.16 HTTP/1.1")

        assertTrue(sanitized.contains("--> GET https://host/rest/ping"))
        assertTrue(sanitized.contains("p=$REDACTED_PLACEHOLDER"))
        assertTrue(sanitized.contains("v=1.16"))
        assertFalse(sanitized.contains("secret"))
    }

    @Test
    fun testSensitiveHeadersAreSanitized() {
        val authorization = sanitizeLogMessage("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9")
        assertTrue(authorization.contains(REDACTED_PLACEHOLDER))
        assertFalse(authorization.contains("eyJhbGciOiJIUzI1NiJ9"))

        val embyToken = sanitizeLogMessage("X-Emby-Token: 0123456789abcdef\nother line")
        assertTrue(embyToken.contains("X-Emby-Token: $REDACTED_PLACEHOLDER"))
        assertFalse(embyToken.contains("0123456789abcdef"))
        assertTrue(embyToken.contains("other line"))
    }

    @Test
    fun testBareSensitiveAssignmentInExceptionMessageIsSanitized() {
        val sanitized = sanitizeLogMessage("java.io.IOException: request failed &t=abc123&s=xyz&c=Rhythm")

        assertTrue(sanitized.contains("t=$REDACTED_PLACEHOLDER"))
        assertTrue(sanitized.contains("s=$REDACTED_PLACEHOLDER"))
        assertTrue(sanitized.contains("c=Rhythm"))
        assertFalse(sanitized.contains("abc123"))
    }

    @Test
    fun testBearerTokenOutsideHeaderIsSanitized() {
        val sanitized = sanitizeLogMessage("failed with Bearer eyJhbGciOiJIUzI1NiJ9 for request")

        assertTrue(sanitized.contains("Bearer $REDACTED_PLACEHOLDER"))
        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiJ9"))
    }

    @Test
    fun testNullOrBlankMessage() {
        assertEquals("", sanitizeLogMessage(null))
        assertEquals("", sanitizeLogMessage(""))
    }

    @Test
    fun testSensitiveHeaderListCoversProviderCredentials() {
        assertTrue(SENSITIVE_HEADERS.contains("Authorization"))
        assertTrue(SENSITIVE_HEADERS.contains("Cookie"))
        assertTrue(SENSITIVE_HEADERS.contains("X-Emby-Token"))
    }
}
