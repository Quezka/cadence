package io.github.quezka.cadence

import io.github.quezka.cadence.data.ReleaseFeed
import io.github.quezka.cadence.domain.Version
import io.github.quezka.cadence.domain.newerThan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateTest {
    @Test fun versionsCompareByNumberNotText() {
        assertTrue(Version.parse("v0.10.0")!! > Version.parse("0.9.9")!!)
        assertEquals(0, Version.parse("1.0")!!.compareTo(Version.parse("v1.0.0")!!))
        assertNull(Version.parse("latest"))
    }

    @Test fun onlyANewerReleaseWithAnApkIsOffered() {
        val json = """{"tag_name":"v0.7.0","body":"notes","assets":[
            {"name":"source.zip","browser_download_url":"x","size":1},
            {"name":"Cadence-0.7.0-android.apk","browser_download_url":"https://e/a.apk","size":42}]}"""
        val r = ReleaseFeed.parse(json)!!
        assertEquals("https://e/a.apk", r.apkUrl)
        assertEquals(r, newerThan("0.6.0", r))
        assertNull(newerThan("0.7.0", r))
        assertNull(ReleaseFeed.parse("""{"tag_name":"v0.8.0","assets":[]}"""))
    }
}
