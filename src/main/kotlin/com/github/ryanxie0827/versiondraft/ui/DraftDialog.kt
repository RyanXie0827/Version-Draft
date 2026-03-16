package com.github.ryanxie0827.versiondraft.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.*
import java.awt.event.ItemEvent
import javax.swing.JComponent

class DraftDialog(
    project: Project,
    val existingReqs: List<String> = emptyList(),
    // 【核心新增】：接收 IDEA 解析出的所有子模块名
    val moduleNames: List<String> = emptyList(),
    var version: String = "",
    var moduleName: String = "", // 当前选中的模块名
    var iconTag: String = "💻",
    var codeLanguage: String = "sql",
    var description: String = "",
    var details: String = ""
) : DialogWrapper(project) {

    private lateinit var dialogPanel: DialogPanel

    private val iconLibrary = listOf(
        "💻", "🗄️", "⚙️", "🐛", "✨", "🚀", "📝", "🔨", "🗑️", "🔥", "🔒", "🎨", "🌐", "📦"
    )

    init {
        title = "记录需求变更 (Version Draft)"
        init()
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = panel {

            row("需求/版本 (Req):") {
                val reqField = textField()
                    .bindText(::version)
                    .focused()
                    .align(AlignX.FILL)

                if (existingReqs.isNotEmpty()) {
                    val comboItems = listOf("快速选择已有需求...") + existingReqs
                    comboBox(comboItems).applyToComponent {
                        addActionListener {
                            if (selectedIndex > 0) {
                                reqField.component.text = selectedItem.toString()
                            }
                        }
                    }
                }
            }

            // 【核心新增】：所属模块选择框
            row("所属模块 (Module):") {
                val moduleItems = listOf("无") + moduleNames
                comboBox(moduleItems)
                    .bindItem({ if(moduleName.isEmpty()) "无" else moduleName },
                        { moduleName = if (it == "无") "" else it ?: "" })
                    .applyToComponent {
                        isEditable = true // 支持手动输入前端模块或其他粒度
                    }
                    .comment("自动提取 IDEA 模块，支持手动修改")
            }

            row("语言与图标:") {
                val langCombo = comboBox(listOf("sql", "yaml", "properties", "java", "xml", "json", "sh", "text", "md"))
                    .bindItem({ codeLanguage }, { codeLanguage = it ?: "text" })

                label("  图标库:")

                val iconCombo = comboBox(iconLibrary)
                    .bindItem({ iconTag }, { iconTag = it ?: "💻" })

                langCombo.component.addItemListener { e ->
                    if (e.stateChange == ItemEvent.SELECTED) {
                        val autoIcon = when (e.item.toString()) {
                            "sql" -> "🗄️"
                            "yaml", "properties", "xml" -> "⚙️"
                            "java", "json" -> "💻"
                            "sh" -> "🚀"
                            "md", "text" -> "📝"
                            else -> "💻"
                        }
                        iconCombo.component.selectedItem = autoIcon
                    }
                }
            }

            row("简述 (Desc):") {
                textField()
                    .bindText(::description)
                    .align(AlignX.FILL)
                    .comment("必填项。例如：修复了登录接口空指针异常")
            }

            row("详情/代码 (Details):") {
                textArea()
                    .bindText(::details)
                    .rows(8)
                    .columns(50)
                    .align(AlignX.FILL)
                    .comment("必填项。请贴入具体的变更代码、SQL或配置详情")
            }
        }
        return dialogPanel
    }

    override fun doOKAction() {
        dialogPanel.apply()

        if (version.isBlank()) {
            Messages.showErrorDialog(contentPane, "需求名称或版本号不能为空！", "校验失败")
            return
        }
        if (description.isBlank()) {
            Messages.showErrorDialog(contentPane, "简述内容不能为空！请填写你修改了什么。", "校验失败")
            return
        }
        if (details.isBlank()) {
            Messages.showErrorDialog(contentPane, "详情或代码不能为空！请提供具体的变更内容以备发版核对。", "校验失败")
            return
        }

        super.doOKAction()
    }
}