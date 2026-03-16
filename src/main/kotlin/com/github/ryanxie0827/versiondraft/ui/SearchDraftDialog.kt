package com.github.ryanxie0827.versiondraft.ui

import com.github.ryanxie0827.versiondraft.utils.DraftFileUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

data class SearchRecord(
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
        val displayModule = if (moduleName.isNotEmpty()) "($moduleName) " else ""
        return "$statusIcon 📦$version | [$iconTag] $displayModule$desc"
    }
}

class SearchDraftDialog(
    private val project: Project,
    private val allRecords: List<SearchRecord>,
    private val existingReqs: List<String>,
    private val moduleNames: List<String>
) : DialogWrapper(project) {

    private val searchField = SearchTextField()
    private val listModel = CollectionListModel<SearchRecord>()
    private val resultList = JBList(listModel)

    private val editorFactory = EditorFactory.getInstance()
    private val document = editorFactory.createDocument("请在左侧点击选择一条搜索结果以查看详情...\n\n💡 提示：右键点击记录可进行【编辑】/【删除】/【标记发版】")
    private val viewer = editorFactory.createViewer(document, project)

    init {
        title = "搜索与管理需求 (Search & Manage Drafts)"
        listModel.replaceAll(allRecords)
        init()

        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterRecords()
            override fun removeUpdate(e: DocumentEvent?) = filterRecords()
            override fun changedUpdate(e: DocumentEvent?) = filterRecords()
        })
    }

    private fun filterRecords() {
        val keyword = searchField.text.lowercase()
        if (keyword.isEmpty()) {
            listModel.replaceAll(allRecords)
        } else {
            val filtered = allRecords.filter {
                it.version.lowercase().contains(keyword) ||
                        it.desc.lowercase().contains(keyword) ||
                        it.moduleName.lowercase().contains(keyword) ||
                        it.code.lowercase().contains(keyword)
            }
            listModel.replaceAll(filtered)
        }
    }

    private fun updateEditorContent(code: String, lang: String) {
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

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, 10))
        mainPanel.preferredSize = Dimension(900, 500)
        mainPanel.add(searchField, BorderLayout.NORTH)

        val splitter = JBSplitter(false, 0.4f)

        splitter.firstComponent = JBScrollPane(resultList)
        splitter.secondComponent = viewer.component

        mainPanel.add(splitter, BorderLayout.CENTER)

        resultList.addListSelectionListener {
            val record = resultList.selectedValue
            if (record != null) {
                updateEditorContent(record.code, record.language)
            }
        }

        resultList.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val index = resultList.locationToIndex(e.point)
                    if (index != -1) {
                        resultList.selectedIndex = index
                        val record = resultList.selectedValue ?: return

                        val popupMenu = JPopupMenu()

                        // 【文案升级】：统一为待发版/已发版
                        val toggleText = if (record.isCompleted) "⏳ 标记为待发版 (Pending)" else "🟢 标记为已发版 (Released)"
                        val toggleItem = JMenuItem(toggleText)
                        toggleItem.addActionListener {
                            DraftFileUtil.toggleRecordStatus(project, record.exactRawMarkdown, record.isCompleted)
                            close(OK_EXIT_CODE)
                        }

                        val editItem = JMenuItem("✏️ 编辑记录 (Edit)")
                        val deleteItem = JMenuItem("🗑️ 删除记录 (Delete)")

                        editItem.addActionListener {
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
                                close(OK_EXIT_CODE)
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
                                close(OK_EXIT_CODE)
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

        return mainPanel
    }

    override fun dispose() {
        super.dispose()
        EditorFactory.getInstance().releaseEditor(viewer)
    }

    override fun getPreferredFocusedComponent(): JComponent = searchField.textEditor
}