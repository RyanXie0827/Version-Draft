package com.github.ryanxie0827.versiondraft.ui

import com.github.ryanxie0827.versiondraft.utils.DraftFileUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.charset.StandardCharsets
import javax.swing.JButton
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

data class VersionRecord(val version: String, val isCompleted: Boolean) {
    override fun toString(): String {
        return if (isCompleted) "📦 $version (🟢 已发版)" else "📦 $version"
    }
}

data class SidebarRecord(
    val version: String,
    val time: String,
    val iconTag: String,
    val moduleName: String,
    val desc: String,
    val code: String,
    val language: String,
    val exactRawMarkdown: String,
    val isCompleted: Boolean
) {
    override fun toString(): String {
        val statusIcon = if (isCompleted) "✅" else "⏳"
        val displayTime = if (time.isNotEmpty()) "[$time] " else ""
        val displayModule = if (moduleName.isNotEmpty()) "($moduleName) " else ""
        return "$statusIcon $displayTime[$iconTag] $displayModule$desc"
    }
}

enum class FilterMode { ALL, PENDING, COMPLETED }

class DraftToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())
        var currentFilter = FilterMode.ALL

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
        val btnAdd = JButton("新增记录", AllIcons.General.Add)
        val btnRefresh = JButton("刷新", AllIcons.Actions.Refresh)
        val filterCombo = ComboBox(arrayOf("显示全部", "待发版 (Pending)", "已发版 (Released)"))

        toolbar.add(btnAdd)
        toolbar.add(btnRefresh)
        toolbar.add(filterCombo)
        mainPanel.add(toolbar, BorderLayout.NORTH)

        val splitter = JBSplitter(true, 0.5f)
        mainPanel.add(splitter, BorderLayout.CENTER)

        val rootNode = DefaultMutableTreeNode("所有发版记录")
        val treeModel = DefaultTreeModel(rootNode)
        val tree = Tree(treeModel)
        tree.isRootVisible = false
        splitter.firstComponent = JBScrollPane(tree)

        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("请在上方点击展开并选择一条变更记录...\n\n💡 提示：右键点击需求或记录，可【标记发版】/【编辑】/【删除】")
        val viewer = editorFactory.createViewer(document, project)
        splitter.secondComponent = viewer.component


        fun updateEditorContent(code: String, lang: String) {
            ApplicationManager.getApplication().runWriteAction {
                document.setText(code.ifEmpty { "/* 该记录未提供代码详情 */" })
            }
            val ext = when (lang) {
                "yaml", "yml" -> "yaml"
                "sql" -> "sql"
                "java" -> "java"
                "xml" -> "xml"
                "json" -> "json"
                "properties" -> "properties"
                else -> "txt"
            }
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension(ext)
            (viewer as? EditorEx)?.highlighter =
                EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
        }

        fun compareVersions(v1: String, v2: String): Int {
            val parts1 = v1.replace(Regex("[^0-9.]"), "").split(".").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.replace(Regex("[^0-9.]"), "").split(".").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
            val length = maxOf(parts1.size, parts2.size)
            for (i in 0 until length) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) return p1.compareTo(p2)
            }
            return v1.compareTo(v2)
        }

        fun refreshTree() {
            // 【核心修复 1】：在刷新前，收集用户当前“手动折叠起来”的所有需求
            val collapsedVersions = mutableSetOf<String>()
            for (i in 0 until tree.rowCount) {
                val path = tree.getPathForRow(i)
                val node = path.lastPathComponent as? DefaultMutableTreeNode
                if (node?.userObject is VersionRecord && !tree.isExpanded(path)) {
                    collapsedVersions.add((node.userObject as VersionRecord).version)
                }
            }

            rootNode.removeAllChildren()
            val file = project.guessProjectDir()?.findChild("CHANGELOG.md")

            if (file != null) {
                val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
                val lines = content.lines()

                val versionNodes = mutableListOf<DefaultMutableTreeNode>()
                var currentVersionNode: DefaultMutableTreeNode? = null

                var i = 0
                while (i < lines.size) {
                    val line = lines[i]
                    val trimmed = line.trim()

                    if (trimmed.startsWith("## ")) {
                        var rawVersion = trimmed.removePrefix("## ").trim()
                        val isVersionCompleted = rawVersion.endsWith("✅")
                        val versionStr = rawVersion.removeSuffix("✅").trim()

                        currentVersionNode = DefaultMutableTreeNode(VersionRecord(versionStr, isVersionCompleted))
                        versionNodes.add(currentVersionNode)
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

                        val versionRecord = currentVersionNode?.userObject as? VersionRecord
                        val versionStr = versionRecord?.version ?: "未分类需求"

                        val record = SidebarRecord(versionStr, time, iconTag, moduleName, desc, code, lang, rawMarkdownBuilder.toString(), isCompleted)

                        val shouldAdd = when (currentFilter) {
                            FilterMode.ALL -> true
                            FilterMode.PENDING -> !isCompleted
                            FilterMode.COMPLETED -> isCompleted
                        }

                        if (shouldAdd) {
                            currentVersionNode?.add(DefaultMutableTreeNode(record))
                        }
                    } else {
                        i++
                    }
                }

                versionNodes.sortWith { n1, n2 ->
                    val v1 = (n1.userObject as VersionRecord).version
                    val v2 = (n2.userObject as VersionRecord).version
                    compareVersions(v2, v1)
                }

                for (node in versionNodes) {
                    rootNode.add(node)
                }
            }

            if (currentFilter != FilterMode.ALL) {
                for (i in rootNode.childCount - 1 downTo 0) {
                    val versionNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
                    val versionRecord = versionNode.userObject as? VersionRecord

                    if (currentFilter == FilterMode.PENDING) {
                        if (versionRecord?.isCompleted == true || versionNode.childCount == 0) {
                            rootNode.remove(i)
                        }
                    } else if (currentFilter == FilterMode.COMPLETED) {
                        if (versionRecord?.isCompleted == false && versionNode.childCount == 0) {
                            rootNode.remove(i)
                        }
                    }
                }
            }

            if (rootNode.childCount == 0) {
                rootNode.add(DefaultMutableTreeNode(
                    when(currentFilter) {
                        FilterMode.PENDING -> "太棒了！待发版的记录已全部清空 🎉"
                        FilterMode.COMPLETED -> "暂无已发版的记录，继续加油吧！💪"
                        else -> "暂无记录，请点击上方新增。"
                    }
                ))
            }

            treeModel.reload()

            // 【核心修复 2】：使用动态 while 循环逐级恢复树的状态
            var row = 0
            while (row < tree.rowCount) {
                val path = tree.getPathForRow(row)
                val node = path.lastPathComponent as? DefaultMutableTreeNode
                if (node?.userObject is VersionRecord) {
                    val version = (node.userObject as VersionRecord).version
                    // 如果它在“被折叠”的小本本里，就乖乖关上；否则默认敞开
                    if (collapsedVersions.contains(version)) {
                        tree.collapsePath(path)
                    } else {
                        tree.expandPath(path)
                    }
                }
                row++
            }
        }

        filterCombo.addActionListener {
            currentFilter = when (filterCombo.selectedIndex) {
                1 -> FilterMode.PENDING
                2 -> FilterMode.COMPLETED
                else -> FilterMode.ALL
            }
            refreshTree()
        }

        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            val record = node?.userObject
            if (record is SidebarRecord) {
                updateEditorContent(record.code, record.language)
            } else {
                updateEditorContent("请在上方点击展开并选择一条具体的变更记录...\n\n💡 提示：右键点击需求或记录，可【标记发版】/【编辑】/【删除】", "text")
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val row = tree.getRowForLocation(e.x, e.y)
                    if (row == -1) return
                    tree.setSelectionRow(row)
                    val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return

                    val popupMenu = JPopupMenu()

                    if (node.userObject is VersionRecord) {
                        val vRecord = node.userObject as VersionRecord
                        val version = vRecord.version

                        val toggleText = if (vRecord.isCompleted) "⏳ 撤销发版状态 (Revert)" else "🚀 标记该需求及子项为已发版"
                        val toggleItem = JMenuItem(toggleText)
                        toggleItem.addActionListener {
                            DraftFileUtil.toggleVersionStatus(project, version, !vRecord.isCompleted)
                            refreshTree()
                        }

                        val deleteItem = JMenuItem("🗑️ 删除整个需求块 (Delete)")
                        deleteItem.addActionListener {
                            val input = Messages.showInputDialog(
                                project,
                                "高危操作！这将删除 $version 及其所有记录。\n请输入「确定删除」以继续：",
                                "删除确认",
                                Messages.getWarningIcon()
                            )
                            if (input == "确定删除") {
                                DraftFileUtil.deleteVersion(project, version)
                                refreshTree()
                            } else if (input != null) {
                                Messages.showErrorDialog("输入内容不匹配，删除已取消。", "取消")
                            }
                        }

                        popupMenu.add(toggleItem)
                        popupMenu.addSeparator()
                        popupMenu.add(deleteItem)
                        popupMenu.show(e.component, e.x, e.y)
                    }
                    else if (node.userObject is SidebarRecord) {
                        val record = node.userObject as SidebarRecord

                        val toggleText = if (record.isCompleted) "⏳ 标记为待发版 (Pending)" else "🟢 标记为已发版 (Released)"
                        val toggleItem = JMenuItem(toggleText)
                        toggleItem.addActionListener {
                            DraftFileUtil.toggleRecordStatus(project, record.exactRawMarkdown, record.isCompleted)
                            refreshTree()
                        }

                        val editItem = JMenuItem("✏️ 编辑记录 (Edit)")
                        val deleteItem = JMenuItem("🗑️ 删除记录 (Delete)")

                        editItem.addActionListener {
                            val existingReqs = DraftFileUtil.getExistingRequirements(project)
                            val moduleNames = ModuleManager.getInstance(project).modules.map { it.name }.sorted()

                            val dialog = DraftDialog(
                                project,
                                existingReqs = existingReqs,
                                moduleNames = moduleNames,
                                version = record.version,
                                moduleName = record.moduleName,
                                iconTag = record.iconTag,
                                codeLanguage = record.language,
                                description = record.desc,
                                details = record.code
                            )
                            if (dialog.showAndGet()) {
                                DraftFileUtil.deleteRecord(project, record.exactRawMarkdown)
                                DraftFileUtil.appendRecord(project, dialog.version, dialog.moduleName, dialog.iconTag, dialog.codeLanguage, dialog.description, dialog.details, record.isCompleted)
                                refreshTree()
                            }
                        }

                        deleteItem.addActionListener {
                            val res = Messages.showYesNoDialog(
                                project,
                                "确定要永久删除这条记录吗？\n\n【${record.desc}】",
                                "删除确认",
                                "确定", "取消",
                                Messages.getWarningIcon()
                            )
                            if (res == Messages.YES) {
                                DraftFileUtil.deleteRecord(project, record.exactRawMarkdown)
                                refreshTree()
                            }
                        }

                        popupMenu.add(toggleItem)
                        popupMenu.addSeparator()
                        popupMenu.add(editItem)
                        popupMenu.add(deleteItem)
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })

        btnAdd.addActionListener {
            val existingReqs = DraftFileUtil.getExistingRequirements(project)
            val moduleNames = ModuleManager.getInstance(project).modules.map { it.name }.sorted()

            val dialog = DraftDialog(project, existingReqs = existingReqs, moduleNames = moduleNames)

            if (dialog.showAndGet()) {
                DraftFileUtil.appendRecord(project, dialog.version, dialog.moduleName, dialog.iconTag, dialog.codeLanguage, dialog.description, dialog.details)
                refreshTree()
            }
        }
        btnRefresh.addActionListener { refreshTree() }

        refreshTree()

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}