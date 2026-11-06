package chromahub.rhythm.app.util

import java.util.Locale

/**
 * Single source of truth for redacting credentials out of network logs.
 *
 * Credentials in this app travel both in URL query parameters (Subsonic `u`/`p`/`t`/`s`,
 * Jellyfin `ApiKey`/`api_key`) and in headers (`Authorization`, `Cookie`, `X-Emby-Token`).
 * OkHttp's `redactHeader` only covers headers, so every log line that may contain a URL,
 * a header or an exception message must pass through [sanitizeLogMessage].
 */
const val REDACTED_PLACEHOLDER = "<redacted>"

val SENSITIVE_QUERY_KEYS: Set<String> = setOf(
    "u", "p", "t", "s",
    "api_key", "apikey", "key", "token", "access_token", "refresh_token",
    "auth", "authorization", "password", "passwd", "pass", "secret",
    "sig", "signature", "csrf_token", "csrf", "cookie", "session",
    "sessionid", "sid", "bearer", "jwt"
)

val SENSITIVE_HEADERS: List<String> = listOf(
    "Authorization", "Cookie", "Set-Cookie",
    "X-Emby-Token", "X-Emby-Authorization", "X-MediaBrowser-Token"
)

private val URL_IN_MESSAGE = Regex("https?://[^\\s\"'<>),;]+")

private val SENSITIVE_ASSIGNMENT = Regex(
    "(?i)([?&;])(" +
        SENSITIVE_QUERY_KEYS.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } +
        ")=([^&\\s\"'<>]*)"
)

private val SENSITIVE_HEADER_LINE = Regex(
    "(?i)\\b(" + SENSITIVE_HEADERS.joinToString("|") { Regex.escape(it) } + "):\\s*[^\\r\\n]*"
)

private val BEARER_TOKEN = Regex("(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]+")

/**
 * Masks the value of every sensitive query parameter while keeping the rest of the URL
 * (scheme, host, path, non-sensitive parameters and fragment) readable for diagnostics.
 */
fun redactUrl(url: String): String {
    val queryStart = url.indexOf('?')
    if (queryStart < 0) return url

    val fragmentStart = url.indexOf('#', queryStart).let { if (it < 0) url.length else it }
    val query = url.substring(queryStart + 1, fragmentStart)
    if (query.isEmpty()) return url

    val redacted = query.split('&').joinToString("&") { part ->
        val name = part.substringBefore('=')
        if (name.lowercase(Locale.ROOT) in SENSITIVE_QUERY_KEYS) {
            "$name=$REDACTED_PLACEHOLDER"
        } else {
            part
        }
    }
    return url.substring(0, queryStart) + "?" + redacted + url.substring(fragmentStart)
}

/**
 * Sanitizes a whole log line: embedded URLs, sensitive query assignments, sensitive
 * header values and bearer tokens. Always use this before handing a network-derived
 * string (including `Throwable.message`) to `Log`.
 */
fun sanitizeLogMessage(message: String?): String {
    if (message.isNullOrEmpty()) return ""

    val urlsRedacted = URL_IN_MESSAGE.replace(message) { redactUrl(it.value) }
    val headersRedacted = SENSITIVE_HEADER_LINE.replace(urlsRedacted) { match ->
        match.value.substringBefore(':') + ": " + REDACTED_PLACEHOLDER
    }
    val queryRedacted = SENSITIVE_ASSIGNMENT.replace(headersRedacted) { match ->
        match.groupValues[1] + match.groupValues[2] + "=" + REDACTED_PLACEHOLDER
    }
    return BEARER_TOKEN.replace(queryRedacted) { match ->
        match.groupValues[1] + " " + REDACTED_PLACEHOLDER
    }
}
