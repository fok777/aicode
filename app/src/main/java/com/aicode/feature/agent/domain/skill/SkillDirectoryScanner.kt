package com.aicode.feature.agent.domain.skill

import java.io.File

/**
 * 目录型技能源的共享扫描逻辑：递归查找 SKILL.md / CLAUDE.md，
 * 每个含指令文件的目录解析为一个 Skill。
 */
object SkillDirectoryScanner {
    /**
     * 扫描 [root] 目录下所有合法技能，按名称排序。
     * 目录不存在时返回空列表。
     */
    fun scan(root: File): List<Skill> {
        if (!root.exists()) return emptyList()
        val skillFiles = root.walkTopDown()
            .maxDepth(4) // 允许一定的嵌套深度（比如 repo/skills/my-skill/SKILL.md）
            .filter { it.isFile && (it.name.equals("SKILL.md", ignoreCase = true) || it.name.equals("CLAUDE.md", ignoreCase = true)) }
            .toList()

        return skillFiles.mapNotNull { file -> file.parentFile }
            .distinct() // 如果同一个目录下同时存在这两种文件，只解析一次
            .mapNotNull { dir -> SkillParser.parse(dir) }
            .sortedBy { it.name.lowercase() }
    }
}
