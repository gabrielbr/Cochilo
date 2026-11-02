package br.gabriel.cochilo.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRulesTest {
    private val sha = "a".repeat(64)

    private fun release(
        tag: String = "v0.2.0",
        prerelease: Boolean = false,
        draft: Boolean = false,
        apk: String? = "cochilo-0.2.0.apk",
        sums: Boolean = true,
        scheme: String = "https",
    ): String {
        val assets = buildList {
            if (apk != null) add("""{"name":"$apk","browser_download_url":"$scheme://github.com/gabrielbr/Cochilo/releases/download/$tag/$apk","size":6755011}""")
            add("""{"name":"cochilo-play-0.2.0.aab","browser_download_url":"https://github.com/x.aab","size":5000000}""")
            if (sums) add("""{"name":"SHA256SUMS.txt","browser_download_url":"$scheme://github.com/gabrielbr/Cochilo/releases/download/$tag/SHA256SUMS.txt","size":90}""")
        }.joinToString(",")
        return """{"tag_name":"$tag","name":"Cochilo","body":"What's new","draft":$draft,"prerelease":$prerelease,"assets":[$assets],"extra":1}"""
    }

    @Test fun versions_parse() {
        assertEquals(listOf(0, 2, 0), UpdateRules.parseVersion("v0.2.0"))
        assertEquals(listOf(1, 10), UpdateRules.parseVersion("1.10"))
        assertEquals(listOf(0, 1, 0), UpdateRules.parseVersion("0.1.0-debug"))
        assertNull(UpdateRules.parseVersion("latest"))
        assertNull(UpdateRules.parseVersion("1.x.0"))
    }

    @Test fun newer_is_strict_and_numeric() {
        assertTrue(UpdateRules.isNewer("0.2.0", "0.1.1"))
        assertTrue(UpdateRules.isNewer("0.10.0", "0.9.9"))
        assertTrue(UpdateRules.isNewer("1.0.1", "1.0"))
        assertFalse(UpdateRules.isNewer("0.1.1", "0.1.1"))
        assertFalse(UpdateRules.isNewer("1.0", "1.0.0"))
        assertFalse(UpdateRules.isNewer("0.1.0", "0.2.0"))
        assertFalse(UpdateRules.isNewer("nonsense", "0.1.0"))
    }

    @Test fun offers_a_newer_release_with_its_apk_and_sums() {
        val e = UpdateRules.evaluate(release(), "0.1.1")
        assertTrue(e is Evaluation.Available)
        val info = (e as Evaluation.Available).info
        assertEquals("0.2.0", info.versionName)
        assertEquals("cochilo-0.2.0.apk", info.apkName)
        assertEquals(6755011L, info.apkSize)
        assertTrue(info.sumsUrl.endsWith("SHA256SUMS.txt"))
        assertEquals("What's new", info.notes)
    }

    @Test fun the_sheet_shows_only_the_first_paragraph() {
        val body = release().replace("\"body\":\"What's new\"", "\"body\":\"Bedtime is easier to read.\\r\\n\\r\\nInstall cochilo-0.2.0.apk on the phone.\"")
        val info = (UpdateRules.evaluate(body, "0.1.0") as Evaluation.Available).info
        assertEquals("Bedtime is easier to read.", info.notes)
    }

    @Test fun never_offers_equal_older_draft_or_prerelease() {
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(), "0.2.0"))
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(), "0.3.0"))
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(prerelease = true), "0.1.0"))
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(draft = true), "0.1.0"))
    }

    @Test fun a_release_without_our_apk_or_sums_is_no_update() {
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(apk = null), "0.1.0"))
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(apk = "cochilo-play-0.2.0.apk"), "0.1.0"))
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(sums = false), "0.1.0"))
    }

    @Test fun plain_http_links_are_refused() {
        assertEquals(Evaluation.UpToDate, UpdateRules.evaluate(release(scheme = "http"), "0.1.0"))
        assertFalse(UpdateRules.isHttps("http://example.com"))
        assertTrue(UpdateRules.isHttps("HTTPS://example.com"))
    }

    @Test fun a_bad_reply_is_a_failure_not_up_to_date() {
        assertTrue(UpdateRules.evaluate("<html>rate limited</html>", "0.1.0") is Evaluation.Invalid)
    }

    @Test fun sums_parse_in_sha256sum_format() {
        val sums = UpdateRules.parseSums("$sha  cochilo-0.2.0.apk\n${"B".repeat(64)} *cochilo-play-0.2.0.aab\nnot a line\n")
        assertEquals(sha, sums["cochilo-0.2.0.apk"])
        assertEquals("b".repeat(64), sums["cochilo-play-0.2.0.aab"])
        assertEquals(2, sums.size)
    }

    @Test fun hex_is_lowercase_two_digits() {
        assertEquals("00ff10", UpdateRules.hex(byteArrayOf(0, -1, 16)))
    }
}
