# 关于 (About)

“设置”首页「关于」入口（图标为信息圆圈）。点击进入后展示下列信息：

*   **应用信息**：顶部卡片左侧显示 App 图标，右上为应用名 `AiCode`，右下为一句简介。
*   **版本（点击检查更新）**：分组内一行，显示当前版本号 `v<versionName>`（通过系统 PackageManager 读取，即对外发布版本，与 git tag `v<versionName>` 一致）。点击该行手动检查更新：通过 GitHub API `https://api.github.com/repos/jieapi/aicode/releases` 查询发布列表，过滤出高于当前版本的版本（按所选更新通道），弹窗展示「发现新版本」并直接列出**从当前版本到最新版本的更新日志**（自动清理 GitHub 自动生成的 “What's Changed” / “Full Changelog” 行；正式版日志已合并两个正式版之间的全部变更）；点击「前往下载」用浏览器打开 GitHub Releases 页面获取最新 APK。若已是最新 → 提示「已经是最新版本」；网络失败或解析失败 → 提示「检查失败」并给出错误信息。
*   **自动检查更新（开关，默认开启）**：打开 App 进入主页时自动异步检测新版本，**每天最多一次**（当天检测过则不再重复）；检测到新版本自动弹出更新弹窗；网络失败时静默不打扰。关闭后不再自动检测（手动点击版本行仍可检查）。
*   **更新通道**：选择检测范围，默认「稳定版」：
    *   **稳定版**：仅检测正式版 Release（过滤 RC 预览版）。
    *   **最新版**：包含预览版（RC）更新。
*   **GitHub 仓库**：点击用系统浏览器打开本项目源码仓库 `https://github.com/jieapi/aicode`。
*   **开源许可证**：本项目使用 **GPL-3.0**，点击用浏览器打开完整 LICENSE 文件。

## 供 AI 读取的版本信息文件

每次检查更新（自动或手动）完成后，App 会把版本与更新信息写入容器内 `~/.aicode/update-info.json`（宿主私有目录 `filesDir/aicode/update-info.json`）。**AI 在回答「当前版本 / 是否有新版本 / 最近更新了什么」等问题时，应读取该文件**，其中包含：

*   `currentVersion`：当前安装版本。
*   `channel`：更新通道（`stable` / `latest`）。
*   `lastCheckedAt`：最近一次检查时间。
*   `hasUpdate`：是否有更新。
*   `latestVersion`：最新版本号（有更新时）。
*   `updates`：逐版本更新日志数组（`tag` + `changelog`）。
*   `error`：最近一次检查失败的错误信息（如有）。
