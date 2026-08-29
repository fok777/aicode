package com.aicode.feature.agent.domain.skill

import com.aicode.feature.agent.domain.container.ContainerInstaller
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局技能来源：`aicodeDir/skills`（容器内 `/root/.aicode/skills`），跨项目、跨升级保留。
 */
@Singleton
class LocalDirectorySkillSource @Inject constructor(
    private val containerInstaller: ContainerInstaller
) : SkillSource {

    val skillsRoot: File by lazy {
        File(containerInstaller.aicodeDir, "skills").also { it.mkdirs() }
    }

    override fun listSkills(): List<Skill> = SkillDirectoryScanner.scan(skillsRoot)

    override fun loadInstructions(name: String): String? =
        listSkills().firstOrNull { it.name.equals(name, ignoreCase = true) }?.instructions
}
