package com.github.ryanxie0827.versiondraft.actions

import com.github.ryanxie0827.versiondraft.ui.SearchRecord
import com.github.ryanxie0827.versiondraft.ui.SearchDraftDialog
import com.github.ryanxie0827.versiondraft.utils.DraftFileUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import java.nio.charset.StandardCharsets

class SearchDraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val baseDir = project.guessProjectDir() ?: return
        val changelogFile = baseDir.findChild("CHANGELOG.md")

        if (changelogFile == null) {
            Messages.showInfoMessage(project, "暂未发现 CHANGELOG.md 文件，请先记录一次需求变更。", "无记录")
            return
        }

        // 获取用于传递给编辑弹窗的基础数据
        val existingReqs = DraftFileUtil.getExistingRequirements(project)
        val moduleNames = ModuleManager.getInstance(project).modules.map { it.name }.sorted()

        val content = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
        val lines = content.lines()

        val records = mutableListOf<SearchRecord>()
        var currentVersion = "Unknown"

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.startsWith("## ")) {
                currentVersion = trimmed.removePrefix("## ").substringBefore("✅").trim()
                i++
            }
            else if (trimmed.startsWith("- **[") || trimmed.startsWith("- [ ] **[") || trimmed.startsWith("- [x] **[")) {

                val isCompleted = trimmed.startsWith("- [x]")
                val rawMarkdownBuilder = StringBuilder()
                rawMarkdownBuilder.append(line).append("\n")

                val normalizedLine = trimmed.replace("- [ ] ", "- ").replace("- [x] ", "- ")
                val titleRegex = Regex("""- \*\*\[(.*?)\] \[(.*?)\] (?:\((.*?)\)\s+)?(.*?)\*\*""")
                val match = titleRegex.find(normalizedLine)

                var time = ""
                var iconTag = "💻"
                var moduleName = ""
                var desc = normalizedLine

                if (match != null) {
                    time = match.groups[1]?.value ?: ""
                    iconTag = match.groups[2]?.value ?: "💻"
                    moduleName = match.groups[3]?.value ?: ""
                    desc = match.groups[4]?.value ?: ""
                } else {
                    desc = normalizedLine.removePrefix("- ").replace("**", "")
                }

                var code = ""
                var lang = "text"
                i++

                // 提取代码块
                if (i < lines.size && lines[i].trim().startsWith("```")) {
                    rawMarkdownBuilder.append(lines[i]).append("\n")
                    lang = lines[i].trim().removePrefix("```").trim()
                    if (lang.isEmpty()) lang = "text"
                    i++

                    val codeBuilder = StringBuilder()
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        rawMarkdownBuilder.append(lines[i]).append("\n")
                        val codeLine = if (lines[i].startsWith("  ")) lines[i].substring(2) else lines[i]
                        codeBuilder.append(codeLine).append("\n")
                        i++
                    }
                    code = codeBuilder.toString().trimEnd()

                    if (i < lines.size && lines[i].trim().startsWith("```")) {
                        rawMarkdownBuilder.append(lines[i]).append("\n")
                        i++
                    }
                }

                records.add(SearchRecord(currentVersion, time, iconTag, moduleName, desc, code, lang, rawMarkdownBuilder.toString(), isCompleted))
            } else {
                i++
            }
        }

        if (records.isEmpty()) {
            Messages.showInfoMessage(project, "CHANGELOG.md 中暂无结构化的发版记录。", "无记录")
            return
        }

        // 弹窗尺寸变大了，展示更加专业
        val dialog = SearchDraftDialog(project, records, existingReqs, moduleNames)
        dialog.show()
    }
}