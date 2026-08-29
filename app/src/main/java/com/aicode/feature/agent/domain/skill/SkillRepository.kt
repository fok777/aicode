package com.aicode.feature.agent.domain.skill

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Skill 仓库，聚合各 [SkillSource]（全局目录 + 项目目录）提供的技能，
 * 并按 [SkillConfigRepository] 的禁用名单过滤注入清单。
 */
@Singleton
class SkillRepository @Inject constructor(
    private val localDirectorySkillSource: LocalDirectorySkillSource,
    private val projectDirectorySkillSource: ProjectDirectorySkillSource,
    private val skillConfigRepository: SkillConfigRepository
) {
    /** 全部技能（含来源作用域），未过滤禁用；同名技能项目级优先（与 MCP 两级配置一致）。 */
    fun listAllSkills(): List<SkillEntry> =
        mergeAll(localDirectorySkillSource.listSkills(), projectDirectorySkillSource.listSkills())

    /** 启用的技能列表（注入系统提示词用），禁用技能被过滤。 */
    fun listSkills(): List<Skill> =
        filterDisabled(listAllSkills(), skillConfigRepository.disabledNames()).map { it.skill }

    /** 读取指定 skill 的完整指令正文；不存在 / 解析失败 / 已被禁用时返回 null。 */
    fun loadInstructions(name: String): String? {
        if (name.lowercase() in skillConfigRepository.disabledNames()) return null
        return localDirectorySkillSource.loadInstructions(name)
            ?: projectDirectorySkillSource.loadInstructions(name)
    }

    /** 技能是否在任一作用域中被禁用。 */
    fun isSkillDisabled(name: String): Boolean =
        name.lowercase() in skillConfigRepository.disabledNames()

    /** 在指定作用域启用/禁用某个技能。 */
    fun setSkillDisabled(name: String, disabled: Boolean, scope: SkillScope) =
        skillConfigRepository.setDisabled(name, disabled, scope)

    /** 删除指定作用域的技能（删除其目录，不可恢复）。返回是否成功。 */
    fun deleteSkill(name: String, scope: SkillScope): Boolean {
        val entry = listAllSkills().firstOrNull {
            it.skill.name.equals(name, ignoreCase = true) && it.scope == scope
        } ?: return false
        val dir = entry.skill.dir ?: return false
        return safeDeleteSkillDir(dir)
    }

    companion object {
        /** 合并两级来源：同名项目级覆盖全局，按名称排序。 */
        internal fun mergeAll(global: List<Skill>, project: List<Skill>): List<SkillEntry> {
            val byName = LinkedHashMap<String, SkillEntry>()
            global.forEach { byName[it.name.lowercase()] = SkillEntry(it, SkillScope.GLOBAL) }
            project.forEach { byName[it.name.lowercase()] = SkillEntry(it, SkillScope.PROJECT) }
            return byName.values.sortedBy { it.skill.name.lowercase() }
        }

        /** 过滤禁用技能（禁用名单已归一化为小写）。 */
        internal fun filterDisabled(entries: List<SkillEntry>, disabled: Set<String>): List<SkillEntry> =
            entries.filterNot { it.skill.name.lowercase() in disabled }

        /** 仅当目录存在且含 SKILL.md/CLAUDE.md 指令文件时才删除，避免误删非技能目录。 */
        internal fun safeDeleteSkillDir(dir: File): Boolean {
            if (!dir.isDirectory) return false
            val hasInstruction = dir.listFiles()?.any {
                it.isFile && (it.name.equals("SKILL.md", ignoreCase = true) || it.name.equals("CLAUDE.md", ignoreCase = true))
            } ?: false
            if (!hasInstruction) return false
            return dir.deleteRecursively()
        }
    }
}
