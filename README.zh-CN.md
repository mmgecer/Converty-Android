# Converty

[English](README.md) | [Türkçe](README.tr.md) | [Deutsch](README.de.md) | 简体中文 | [العربية](README.ar.md) | [Português](README.pt.md) | [Français](README.fr.md) | [Русский](README.ru.md)

Converty 是一款在 Android 设备本地完成文件转换的应用。项目使用 Android Storage Access Framework、WorkManager 和 Material 3 Expressive 界面，支持单文件与批量文件、指定页面或幻灯片、持久化转换历史，以及直接打开或分享输出文件。

本项目仍在积极开发。PDF 转 PPTX 会把渲染后的页面作为图片放入幻灯片，因此能保留页面外观，但文字不可编辑。PPTX 转 PDF 和 Office 转 PDF 只支持文档格式中的实用子集，不宣称能够完整复现 Microsoft Office 的渲染结果。

## 当前状态

- Android 应用 ID：`com.converty.app`
- 最低版本：Android 6.0，API 23
- 目标与编译 SDK：API 37
- 界面：Jetpack Compose 与 Material 3 Expressive
- 处理方式：完全在设备本地执行
- 当前验证：80 项 JVM 测试和 7 项 Android 真机 instrumentation 测试
- 公开发布状态：源码已按 Apache-2.0 许可准备好公开发布，并已生成本地签名的 release；F-Droid 准备工作尚未完成。

## 主要功能

- 在同一流程中选择一个或多个文件。
- 通过 WorkManager 在后台转换，并显示任务级和文件级进度。
- 在本地保存成功、失败和取消的转换历史。
- 从历史记录分别打开或分享每个输出文件。
- 通过 Android 系统文件选择器指定输出目录。
- 处理全部、前 N 个、后 N 个页面或幻灯片。
- 支持 `1,3-5,9` 等不连续范围，并可选择保留或排除范围。
- 在适用方向中设置图像质量、DPI、WebP 无损模式和 PDF 转 PPTX 的图像适配方式。
- 支持系统、浅色、深色、动态颜色和多种品牌配色。
- 支持英语、土耳其语、德语、简体中文、阿拉伯语、葡萄牙语、法语和俄语。
- 支持自适应、圆形和 Android 13 单色主题图标。

## 支持的转换

### 文档与演示文稿

| 源格式 | 目标格式 | 说明 |
|---|---|---|
| PDF | PPTX | 将每个选定页面渲染为图片并放入幻灯片；文字不可编辑。 |
| PPTX | PDF | 支持基础文字、图片、填充、线条和形状；部分功能可能产生警告。 |
| DOCX | PDF | 支持基础文档内容；复杂 Word 排版可能不同。 |
| XLSX | PDF | 支持基础工作表内容；高级排版和计算行为有限。 |
| ODT、ODS、ODP | PDF | 在设备本地渲染受支持的 OpenDocument 内容。 |

### PDF 与图像输出

- PDF 转 PNG、JPG、TIFF 和 JPEG 缩略图。
- PNG 转 JPG 或 WebP。
- JPG 转 PNG 或 WebP。
- WebP 转 PNG 或 JPG。
- TIFF、BMP 转 PNG 或 JPG。
- HEIC、HEIF 转 PNG 或 JPG。
- SVG 转 PNG。
- AVIF 转 PNG、JPG 或 WebP。

HEIC、HEIF、AVIF、TIFF 和 WebP 的实际 codec 支持可能因 Android 版本和设备实现而异。

## 页面选择与输出控制

PDF 和 PPTX 输入可选择全部、前 N 个、后 N 个、明确保留的范围、明确排除的范围，或一次选择多个独立范围。PDF 转 PPTX 提供 contain、cover 和 stretch 三种适配方式。适用的 PDF 栅格转换支持 72 至 600 DPI。输出名称会被安全处理，同时保留 Unicode 文件名。

## 隐私与存储

当前应用未声明 Android `INTERNET` 权限，也没有广告或分析 SDK。所有转换都在设备上完成。应用只能访问用户通过 Android 系统文件选择器明确选择的文件和输出目录。

转换历史和设置通过 Room 与 DataStore 保存在本地，并被排除在 Android 云备份和设备迁移备份之外。应用配置禁止明文网络流量。如果以后加入新权限、网络、遥测或崩溃上报功能，应重新审核这些说明。

## 已知限制

- PDF 转 PPTX 生成基于图片的幻灯片，不会重建可编辑的 PowerPoint 对象。
- PPTX 转 PDF 不能替代 Microsoft PowerPoint、LibreOffice 或完整 OOXML 渲染引擎。SmartArt、图表、表格、组合对象、媒体、动画、母版、复杂主题、字体替换和高级效果可能不完整或不受支持。
- DOCX、XLSX 和 OpenDocument 使用有限的本地渲染模型，不能保证精确分页和版式。
- 加密、损坏、异常巨大或恶意构造的文件可能被拒绝。
- 大型文件可能消耗较多内存和时间。
- Android 13 及以上的前台任务需要通知权限；拒绝权限不会禁用应用内队列。

## 从源码构建

需要 JDK 17、Android SDK Platform 37、Build Tools 37.0.0，以及项目自带的 Gradle Wrapper。主要工具版本为 AGP 9.2.1、Gradle 9.4.1、Kotlin 2.3.10、Compose BOM 2026.06.00 和 Material 3 1.5.0-alpha23。

Linux 或 macOS：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Release 变体：

```sh
./gradlew assembleRelease
```

Production 签名通过本地 `key.properties` 和专用 release keystore 配置。这两个文件均由 `.gitignore` 排除，绝不能提交到仓库，也不能复制到文档、日志或 issue 中。存在这些本地文件时，`assembleRelease` 会生成已签名 APK。干净的公开源码副本不包含任何私有签名材料，可以生成未签名 release；这是适用于 F-Droid 的正确源码构建方式，因为 F-Droid 会自行签名其分发的二进制文件。

## 项目结构

- `core`：转换协议、选择逻辑与转换引擎。
- `data`：Storage Access Framework、Room 历史和 DataStore 设置。
- `work`：WorkManager、进度、取消和 worker 适配器。
- `feature` 与 `ui`：Compose 页面和 Material 3 Expressive 设计系统。
- `res`：本地化文本、主题、图标与 Android 配置。
- `src/test` 与 `src/androidTest`：JVM 测试和设备测试。
- `HANDOFF`：架构、决策与验证记录。

## 发布状态

源码树已经按 Apache License 2.0 准备好发布到 GitHub。专用 production 密钥仅在本地配置，并且已经生成已签名的 release APK。私有签名文件有意不包含在公开源码树中。

当前已签名 APK 应视为本地 release 候选版本，而不是已满足 F-Droid 要求的软件包。在提交 F-Droid 或永久公开发布该软件包之前，仍需完成以下工作：

1. 将 `com.converty.app` 替换为永久且全球唯一的应用 ID。该 ID 已被一个无关应用使用，按现状发布会造成应用身份冲突。
2. 本地 `main` 历史和首次 commit 现已存在。仍需配置目标 GitHub remote、push `main`，并创建和 push 与提交源码完全一致的 `v0.1.0` tag。
3. 添加 F-Droid 构建元数据，包括版本映射和完全从源码构建的配方。
4. 从干净的 Linux checkout 在类似 F-Droid 的环境中重新构建，并通过 F-Droid scanner 与可复现性检查。
5. 完成资源与依赖许可证的最终审查，并补充审查确认需要的声明或署名。
6. 每次发布都提高 `versionCode`，并公开发行说明、校验值、截图、隐私说明以及支持或 issue 渠道。

Apache-2.0 许可和本地 production 签名要求已经解决。应用 ID 冲突、GitHub remote 与 push 以及 `v0.1.0` tag、F-Droid 元数据，以及干净的 Linux/F-Droid 构建和 scanner 验证仍是阻塞项。

## 许可证

Converty 采用 Apache License 2.0。完整条款请参阅根目录中的 [`LICENSE`](LICENSE) 文件。
