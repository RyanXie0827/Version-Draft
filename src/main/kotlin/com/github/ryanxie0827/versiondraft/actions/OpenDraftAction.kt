package com.github.ryanxie0827.versiondraft.actions

import com.github.ryanxie0827.versiondraft.ui.DraftDialog
import com.github.ryanxie0827.versiondraft.utils.DraftFileUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import java.nio.charset.StandardCharsets

class OpenDraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val baseDir = project.guessProjectDir() ?: return

        // 获取需求列表
        val existingReqs = DraftFileUtil.getExistingRequirements(project)
        // 【核心新增】：获取当前项目的所有模块名称并排序
        val moduleNames = ModuleManager.getInstance(project).modules.map { it.name }.sorted()

        val dialog = DraftDialog(project, existingReqs = existingReqs, moduleNames = moduleNames)

        if (dialog.showAndGet()) {
            val version = dialog.version

            val changelogFile = baseDir.findChild("CHANGELOG.md")
            var versionExists = false

            if (changelogFile != null) {
                val existingContent = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8)
                val versionRegex = Regex("(?m)^##\\s+${Regex.escape(version)}(?:\\s+✅)?\\s*$")
                if (versionRegex.containsMatchIn(existingContent)) {
                    versionExists = true
                }
            }

            if (versionExists) {
                val result = Messages.showYesNoDialog(
                    project,
                    "文档中已存在需求/版本 【$version】。\n继续提交将会把本次记录归类到该需求下方。\n\n是否继续归类？",
                    "归类确认",
                    "确认归类",
                    "取消",
                    Messages.getInformationIcon()
                )

                if (result != Messages.YES) {
                    return
                }
            }

            DraftFileUtil.appendRecord(
                project,
                dialog.version,
                dialog.moduleName, // 【核心新增】：传入模块名
                dialog.iconTag,
                dialog.codeLanguage,
                dialog.description,
                dialog.details
            )
        }
    }
}