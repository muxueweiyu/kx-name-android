# 江湖大侠 Android WebView 壳子游戏项目 (kx-name-android)

这是一个基于谷歌原生 `WebView` 封装的《江湖大侠》Android 套壳客户端项目。

## 🌟 核心特性
*   **屏幕常亮 (Keep Screen On)**：防止手机在离线挂机时自动锁屏，完美契合挂机需求。
*   **DOM Storage 开启**：开启完整的 DOM 与 Web Database 存储权限，确保 H5 游戏的本地数据存档不丢失。
*   **物理返回键拦截**：拦截 Android 物理返回键（或全面屏返回手势），使其在游戏网页内执行“后退”动作，而不是直接闪退关闭应用。
*   **放行非加密 HTTP 流量**：配置 `usesCleartextTraffic="true"`，绕过 Android 9.0+ 默认强行阻拦非 HTTPS 网络请求的限制。
*   **自适应全屏 (沉浸模式)**：完全隐藏 Android 状态栏与标题栏，最大化利用屏幕空间渲染游戏。

---

## 🛠️ Android Studio 打开与配置指南

### 1. 导入项目
1.  启动 **Android Studio**。
2.  点击 **File -> Open**（或在欢迎界面点击 **Open**）。
3.  选中当前工程的根目录文件夹：**`kx-name-android`**，点击 **OK** 导入。
4.  等待 Gradle 自动构建和依赖下载完成。

### 2. 配置 App 图标 (一键生成)
在 Android 中，建议使用 Android Studio 的官方工具一键生成全分辨率图标，防止图标模糊：
1.  在左侧项目树（Project）中，依次展开：**`app` -> `src` -> `main` -> `res`**。
2.  右键点击 **`res`** 文件夹，选择 **`New -> Image Asset`**。
3.  在弹出的配置窗口中：
    *   **Icon Type**：保持为 `Launcher Icons (Adaptive and Legacy)`。
    *   **Path**：点击右侧文件夹图标，选择我们帮你画好的超清图标：👉 **`C:\cf\diy\test\app_icon.jpg`**。
    *   **Resize**：拉动下方的滑块，将图标调整到绿色的安全圈以内（防止被手机圆角裁剪）。
4.  点击 **`Next`**，然后点击 **`Finish`** 确认。Android Studio 会自动将适配各种安卓屏幕尺寸的图标生成到各分辨率的 `mipmap` 文件夹中。

### 3. 修改 App 名称与包名（可选）
*   **修改 APP 名字**：打开 `app/src/main/res/values/strings.xml`（如果有该文件），或者在 `AndroidManifest.xml` 中，将 `android:label="江湖大侠"` 修改为你喜欢的中文字。
*   **修改包名**：如果想修改 `com.game.shell`，直接在 `AndroidManifest.xml` 和 `MainActivity.java` 中进行包名重构（Refactor -> Rename）即可。

---

## 📦 如何打包并输出 `.apk` 安装包？

1.  在 Android Studio 顶部菜单栏中，点击 **`Build -> Build Bundle(s) / APK(s) -> Build APK(s)`**。
2.  等待右下角提示 `Generate APK... Successfully`。
3.  点击提示里的 **`locate`** 链接，它会自动在电脑资源管理器中打开输出目录：
    👉 通常路径在 `app/build/outputs/apk/debug/app-debug.apk`。
4.  把这个 `app-debug.apk` 发送到任何安卓手机上，即可直接安装畅玩！
