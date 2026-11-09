package io.github.quezka.cadence.data

import io.github.quezka.cadence.domain.Release
import io.github.quezka.cadence.domain.Version
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Reads the newest GitHub release of the repository (public, so no token is needed). */
object ReleaseFeed {
    /** Turns the GitHub "latest release" JSON into a [Release]; null when it carries no .apk. */
    fun parse(json: String): Release? {
        val o = JSONObject(json)
        val version = Version.parse(o.optString("tag_name")) ?: return null
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                return Release(version, o.optString("body"), a.getString("browser_download_url"), a.optLong("size"))
            }
        }
        return null
    }

    fun latest(repo: String): Release? {
        val c = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (c.responseCode == 404) return null          // nothing published yet
            return parse(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }
}
