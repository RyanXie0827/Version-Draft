# Version Draft

一款极轻量级的 IDEA 发版备忘录插件，支持多语言代码高亮与右侧边栏树状图展示。拒绝繁琐的工单系统，让开发者在修改代码和配置的瞬间完成发版记录。

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-2024.1+-green.svg)]()

**Version Draft** 是一款为开发者量身定制的轻量级“发版备忘草稿本” IDEA 插件。
当你在开发过程中顺手建了一张表、改了一行 Nacos 配置或写了一段核心 SQL 时，无需切出 IDE 去填繁琐的工单，只需一个快捷键，即可将这些变更瞬间归档。

---

<p><b>Version Draft</b> 是一款将 Markdown 发挥到极致的极简 IDEA 侧边栏插件。</p>
<p>它完全基于你项目根目录的 <code>CHANGELOG.md</code> 文件，不依赖任何外部数据库，天然完美契合 Git 工作流。不仅是记录工具，它更是你当前版本的 <b>发版 Checklist 看板</b>。</p>

<h3>✨ 核心特性 (Core Features)</h3>
<ul>
  <li>⚡ <b>极速沉浸录入</b>：全局快捷键 <code>Ctrl + Alt + N</code> 一键唤起录入面板，支持版本重复拦截与非空校验，敲完回车瞬间关窗，不打断心流。</li>
  <li>🌳 <b>原生高亮分屏 (杀手锏)</b>：右侧专属 Tool Window，左侧按版本呈现树状列表，右侧调用 IDEA 原生编辑器，完美支持 SQL、YAML、Java、JSON 等多语言的<b>语法高亮呈现</b>。</li>
  <li>✅ <b>Checklist 看板模式</b>：支持将单条记录或整个大版本标记为“已完成 (Done)”。开启<b>“只看待办”</b>过滤器后，已发版的版本和记录将瞬间隐藏，让你极度专注当前任务。</li>
  <li>🔍 <b>极客级搜索</b>：快捷键 <code>Ctrl + Alt + H</code> 唤起全局搜索面板，输入关键字实时秒级过滤历史记录。</li>
  <li>✏️ <b>无缝无痕编辑</b>：在侧边栏右键可对记录进行精准的编辑与删除。如果在编辑时修改了版本号，该记录会自动跨版本“瞬移”归档。</li>
</ul>
---

## 🚀 快捷键指南 (Shortcuts)

| 操作 | Windows / Linux | Mac OS | 说明 |
| :--- | :--- | :--- | :--- |
| **新增记录** | `Ctrl + Alt + N` | `Cmd + Option + N` | 随时随地唤起极简录入弹窗 |
| **搜索历史** | `Ctrl + Alt + H` | `Cmd + Option + H` | 唤起实时过滤的搜索列表 |
| **打开侧边栏** | 鼠标点击右侧 `Version Draft` 标签 | 鼠标点击右侧标签 | 查看全局树状图与高亮代码 |

## 🛠️ 数据存储说明 (Storage)

本插件秉持**“绝对轻量化”**与**“零黑盒”**的极客理念：
- **没有 SQLite，没有云端依赖**。
- 所有的操作都会被严格格式化为标准的 Task List 语法，并静默写入你项目根目录下的 `CHANGELOG.md` 中。
- **Git 友好**：你可以像提交代码一样提交这份记录，团队成员拉取代码后，在 GitHub/GitLab 网页端也能直接看到美观的复选框和高亮代码块。

## 📦 安装指南 (Installation)

### 方法一：通过 ZIP 包本地安装 (推荐尝鲜)
1. 下载本仓库 Releases 中的 `Version-Draft-1.0.0.zip` (请勿解压)。
2. 打开 IDEA，进入 `Settings` (设置) -> `Plugins` (插件)。
3. 点击顶部的 ⚙️ 齿轮图标，选择 **Install Plugin from Disk...** (从磁盘安装插件)。
4. 选中下载的 ZIP 文件，点击 OK，重启 IDEA 即可使用。

### 方法二：通过官方插件市场安装 (即将上线)
1. 打开 IDEA，进入 `Settings` -> `Plugins` -> `Marketplace`。
2. 搜索框输入 **Version Draft**。
3. 点击 Install 安装并重启 IDE。

---

*Powered by Ryan - 专注于提升开发者的日常幸福感。*