package com.aicode.feature.workspace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 远程模式下 AI 路径 ↔ 远程真实路径的映射与 stat 输出解析（[RemoteSftpFileAccess] 的纯逻辑部分）。
 */
class RemoteSftpFileAccessMappingTest {

    private val wsRoot = "/data/user/0/com.aicode/files/projects/demo"
    private val home = "/home/dev"

    // ---------- remotePathFor：AI 路径 → 远程真实路径 ----------

    @Test
    fun workspace_root_maps_to_configured_root() {
        assertEquals(wsRoot, remotePathFor("~/workspace", wsRoot, home))
        assertEquals(wsRoot, remotePathFor("~/workspace/", wsRoot, home))
        assertEquals(wsRoot, remotePathFor("/home/dev/workspace", wsRoot, home))
        assertEquals(wsRoot, remotePathFor("/home/dev/workspace/", wsRoot, home))
    }

    @Test
    fun workspace_child_maps_under_root() {
        assertEquals("$wsRoot/src/Main.kt", remotePathFor("~/workspace/src/Main.kt", wsRoot, home))
        assertEquals("$wsRoot/app/build.gradle.kts", remotePathFor("/home/dev/workspace/app/build.gradle.kts", wsRoot, home))
        assertEquals("$wsRoot/a/b/c.txt", remotePathFor("~/workspace/a/b/c.txt", wsRoot, home))
    }

    @Test
    fun absolute_path_passes_through() {
        assertEquals("/etc/nginx/nginx.conf", remotePathFor("/etc/nginx/nginx.conf", wsRoot, home))
        assertEquals("/root/.aicode/skills/x/SKILL.md", remotePathFor("/root/.aicode/skills/x/SKILL.md", wsRoot, home))
    }

    @Test
    fun relative_path_hangs_under_root() {
        assertEquals("$wsRoot/src/Main.kt", remotePathFor("src/Main.kt", wsRoot, home))
    }

    @Test
    fun input_is_trimmed() {
        assertEquals(wsRoot, remotePathFor("  ~/workspace  ", wsRoot, home))
    }

    @Test
    fun home_unknown_falls_back_to_tilde_form() {
        // remoteHome 未获取到时 ~/workspace 前缀仍能映射（wsRoot 回退为字面 ~/workspace）
        assertEquals(wsRoot, remotePathFor("~/workspace", wsRoot, null))
        assertEquals("$wsRoot/foo.txt", remotePathFor("~/workspace/foo.txt", wsRoot, null))
        // 非工作区前缀的 ~ 路径不会展开，按现状挂到根下
        assertEquals("$wsRoot/~/other", remotePathFor("~/other", wsRoot, null))
    }

    // ---------- displayPathFor：远程真实路径 → AI 视角路径 ----------

    @Test
    fun display_root_maps_to_container_root() {
        assertEquals("~/workspace", displayPathFor(wsRoot, wsRoot))
        assertEquals("~/workspace/src/Main.kt", displayPathFor("$wsRoot/src/Main.kt", wsRoot))
    }

    @Test
    fun display_other_absolute_path_unchanged() {
        assertEquals("/etc/passwd", displayPathFor("/etc/passwd", wsRoot))
    }

    // ---------- shellQuote：单引号转义 ----------

    @Test
    fun shell_quote_wraps_plain_path() {
        assertEquals("'/data/ws/a b.txt'", shellQuote("/data/ws/a b.txt"))
        assertEquals("''", shellQuote(""))
    }

    @Test
    fun shell_quote_escapes_single_quote() {
        assertEquals("'/data/it'\\''s.txt'", shellQuote("/data/it's.txt"))
    }

    // ---------- parseStatEntryLine：stat 输出行解析 ----------

    @Test
    fun parse_stat_regular_file() {
        val entry = parseStatEntryLine("$wsRoot/src/Main.kt|regular file|2048|1700000000|-rw-r--r--")
        assertEquals("Main.kt", entry?.name)
        assertFalse(entry?.isDirectory ?: true)
        assertEquals(2048L, entry?.size)
        assertEquals(1700000000000L, entry?.lastModified)
        assertEquals("rw-r--r--", entry?.permissions)
        assertNull(entry?.localFile)
    }

    @Test
    fun parse_stat_directory() {
        val entry = parseStatEntryLine("$wsRoot/src|directory|4096|1700000000|drwxr-xr-x")
        assertEquals("src", entry?.name)
        assertTrue(entry?.isDirectory ?: false)
        assertEquals("rwxr-xr-x", entry?.permissions)
    }

    @Test
    fun parse_stat_ignores_broken_lines() {
        assertNull(parseStatEntryLine("a|b"))
        assertNull(parseStatEntryLine("|regular file|1|2|3"))
    }

    @Test
    fun parse_stat_filters_dot_entries_keeps_hidden() {
        // .* glob 会带上 . 与 ..，应过滤；隐藏项（.git）保留
        assertNull(parseStatEntryLine("$wsRoot/.|directory|4096|1700000000|drwxr-xr-x"))
        assertNull(parseStatEntryLine("$wsRoot/..|directory|4096|1700000000|drwxr-xr-x"))
        val hidden = parseStatEntryLine("$wsRoot/.git|directory|4096|1700000000|drwxr-xr-x")
        assertEquals(".git", hidden?.name)
    }

    @Test
    fun parse_stat_tolerates_bad_numbers() {
        val entry = parseStatEntryLine("$wsRoot/f.txt|regular file|abc|notatime|---------")
        assertEquals(0L, entry?.size)
        assertEquals(0L, entry?.lastModified)
    }
}
