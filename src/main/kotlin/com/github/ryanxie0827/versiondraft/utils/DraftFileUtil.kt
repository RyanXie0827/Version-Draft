package com.github.ryanxie0827.versiondraft.utils

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object DraftFileUtil {

    fun appendRecord(project: Project, version: String, moduleName: String, iconTag: String, lang: String, desc: String, detail: String, isCompleted: Boolean = false) {
        val baseDir = project.guessProjectDir() ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                var changelogFile = baseDir.findChild("CHANGELOG.md")
                if (changelogFile == null) {
                    changelogFile = baseDir.createChildData(this, "CHANGELOG.md")
                }

                val existingContent = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
                val mdBuilder = StringBuilder()
                val checkbox = if (isCompleted) "- [x]" else "- [ ]"

                val modulePart = if (moduleName.isNotBlank() && moduleName != "无") "($moduleName) " else ""
                mdBuilder.append("$checkbox **[$time] [$iconTag] $modulePart$desc**\n")

                if (detail.trim().isNotEmpty()) {
                    mdBuilder.append("  ```").append(lang).append("\n  ")
                        .append(detail.replace("\n", "\n  "))
                        .append("\n  ```\n")
                }
                val recordText = mdBuilder.toString()

                val versionEscaped = Regex.escape(version)
                val headerRegex = Regex("(?m)^##\\s+$versionEscaped(?:\\s+✅)?\\s*$")
                val headerMatch = headerRegex.find(existingContent)

                if (headerMatch != null) {
                    val headerIndex = headerMatch.range.first
                    var nextHeaderIndex = existingContent.indexOf("\n## ", headerIndex + 2)
                    if (nextHeaderIndex == -1) {
                        nextHeaderIndex = existingContent.length
                    }

                    val newContent = existingContent.substring(0, nextHeaderIndex).trimEnd() +
                            "\n" + recordText + "\n" +
                            existingContent.substring(nextHeaderIndex).trimStart('\n')

                    changelogFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
                } else {
                    val prefix = if (existingContent.trim().isEmpty()) "" else "\n\n"
                    val newContent = existingContent.trimEnd() + prefix + "## $version\n\n" + recordText
                    changelogFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // 【核心升级】：级联修改该版本下的所有子项状态
    fun toggleVersionStatus(project: Project, version: String, markAsCompleted: Boolean) {
        val baseDir = project.guessProjectDir() ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val changelogFile = baseDir.findChild("CHANGELOG.md") ?: return@runWriteCommandAction
                val content = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")

                val versionEscaped = Regex.escape(version)
                val headerRegex = Regex("(?m)^##\\s+$versionEscaped(?:\\s+✅)?\\s*$")
                val headerMatch = headerRegex.find(content) ?: return@runWriteCommandAction

                val startIndex = headerMatch.range.first
                var nextHeaderIndex = content.indexOf("\n## ", startIndex + 2)
                if (nextHeaderIndex == -1) {
                    nextHeaderIndex = content.length
                }

                // 截取当前需求区块的代码
                val oldBlock = content.substring(startIndex, nextHeaderIndex)
                var newBlock = oldBlock

                // 1. 替换大标题
                val newHeader = if (markAsCompleted) "## $version ✅" else "## $version"
                newBlock = newBlock.replaceFirst(headerRegex, newHeader)

                // 2. 批量替换底下所有子项的复选框（使用正则匹配行首，防止误伤代码块里的内容）
                if (markAsCompleted) {
                    newBlock = newBlock.replace(Regex("(?m)^-\\s+\\[\\s\\]\\s+"), "- [x] ")
                } else {
                    newBlock = newBlock.replace(Regex("(?m)^-\\s+\\[x\\]\\s+"), "- [ ] ")
                }

                // 拼接回全文
                val newContent = content.substring(0, startIndex) + newBlock + content.substring(nextHeaderIndex)
                changelogFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleRecordStatus(project: Project, exactRawMarkdown: String, currentStatus: Boolean) {
        val baseDir = project.guessProjectDir() ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val changelogFile = baseDir.findChild("CHANGELOG.md") ?: return@runWriteCommandAction
                val content = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
                val newRecordStr = if (currentStatus) {
                    exactRawMarkdown.replaceFirst("- [x] ", "- [ ] ")
                } else {
                    if (exactRawMarkdown.startsWith("- [ ] ")) {
                        exactRawMarkdown.replaceFirst("- [ ] ", "- [x] ")
                    } else {
                        exactRawMarkdown.replaceFirst("- **", "- [x] **")
                    }
                }
                val newContent = content.replace(exactRawMarkdown, newRecordStr)
                changelogFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteVersion(project: Project, version: String) {
        val baseDir = project.guessProjectDir() ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val changelogFile = baseDir.findChild("CHANGELOG.md") ?: return@runWriteCommandAction
                val content = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
                val versionEscaped = Regex.escape(version)
                val regex = Regex("(?s)\\n*##\\s+$versionEscaped(?:\\s+✅)?\\b.*?(?=\\n## |\\Z)")
                val newContent = content.replace(regex, "").trim() + "\n"
                changelogFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRecord(project: Project, exactRawMarkdown: String) {
        val baseDir = project.guessProjectDir() ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val changelogFile = baseDir.findChild("CHANGELOG.md") ?: return@runWriteCommandAction
                val content = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
                val newContent = content.replace(exactRawMarkdown, "")
                changelogFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getExistingRequirements(project: Project): List<String> {
        val baseDir = project.guessProjectDir() ?: return emptyList()
        val changelogFile = baseDir.findChild("CHANGELOG.md") ?: return emptyList()
        val content = String(changelogFile.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")

        val reqs = mutableListOf<String>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("## ")) {
                val req = trimmed.removePrefix("## ").substringBefore("✅").trim()
                if (req.isNotEmpty() && !reqs.contains(req)) {
                    reqs.add(req)
                }
            }
        }
        return reqs
    }
}