package io.github.quezka.cadence.domain

/** A dotted version number ("v0.6.1"), compared part by part. */
data class Version(val parts: List<Int>) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    override fun toString() = parts.joinToString(".")

    companion object {
        fun parse(text: String): Version? {
            val core = text.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
            if (core.isEmpty()) return null
            val nums = core.split('.').map { it.toIntOrNull() ?: return null }
            return Version(nums)
        }
    }
}

/** A published release with its APK. */
data class Release(val version: Version, val notes: String, val apkUrl: String, val apkSize: Long)

/** The release, if it's newer than the version running now. */
fun newerThan(current: String, latest: Release?): Release? {
    val cur = Version.parse(current) ?: return null
    return latest?.takeIf { it.version > cur }
}
