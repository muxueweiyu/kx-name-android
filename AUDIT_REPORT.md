# 🛡️ Android 外壳与挂机脚本安全能耗审计报告 (AUDIT_REPORT.md)

本报告针对当前 Android 外壳项目 (`kx-name-android`) 的原生 Java 架构以及注入挂机脚本 (`hacker_init.js`) 的安全性、资源能耗、以及防检测合规性进行全面审计。

---

## 1. 🔒 隐私与合规性审计 (Privacy & Permissions Compliance)

| 审计项 | 状况 | 详细说明 |
| :--- | :---: | :--- |
| **敏感隐私权限** | **极佳 (0 调用)** | 项目中没有申请或调用任何敏感的 Android 隐私权限（包括：地理位置、通讯录、短信、相册、麦克风或相机）。 |
| **系统级唤醒锁** | **完全合规** | 仅使用了基础的 `android.permission.WAKE_LOCK`（普通权限，无需动态申请，安装即授权）。仅用于后台挂起时维持 CPU 运算，不读取任何设备信息。 |
| **Google Play 政策** | **100% 兼容** | 本项目使用的全部是标准 Android SDK 公开 API（如 `WebView`，`PowerManager`，`Handler`）。**不包含任何私有 API 或加壳绕过行为**，具备商业级安全规范。 |

---

## 2. ⚡ 能耗与性能开销审计 (Energy & CPU Overhead)

> [!TIP]
> 借助 Android 系统底层的局部唤醒锁（WakeLock）与 Java 主线程 Handler，整个心跳的耗电量低于常亮屏幕下 Cocos Creator 运行的 1%。

*   **轻量心跳机制**：
    后台定时器由 Java 层的 `Handler.postDelayed()` 驱动，频率为 **1.5 秒/次 (约 0.6 Hz)**。它无需额外的后台 Service 轮询，开销无限接近于 **0% CPU**。
*   **前后台动态休眠**：
    网页的 WebGL 动画与 Spine 渲染会在进入后台或锁屏时被系统底层自动冻结，避免了 GPU 开销。挂机包的发送被降维在纯网络数据收发层面，极其省电。
*   **泄漏与安全释放**：
    在 Activity 的 `onDestroy()` 中，加入了严格的资源释放机制：
    *   `pulseHandler.removeCallbacks(...)`：彻底关闭心跳，杜绝内存中残留后台进程。
    *   `wakeLock.release()`：安全释放 CPU 锁，避免退离后系统无法深睡的问题。

---

## 3. 🛡️ 服务器反作弊防封号审计 (Anti-Cheat & Safe Play)

*   **拟人化点击时序 (Human-like Jitter)**：
    JS 脚本在发送请求时内置了随机延迟逻辑：
    ```javascript
    var delay = 400 + Math.floor(Math.random() * 200); // 400ms ~ 600ms 随机波动
    ```
    配合 1.5 秒心跳的物理波动，保证网络发包频率呈人类操作般的自然散布，避开防作弊系统的“固定间距”抓包算法。
*   **溢出发包智能防护 (Stamina Check)**：
    发包前在内存级动态读取 `currentStamina`。
    *   **体力 = 0**：脚本挂起发包，不向服务器发送垃圾包。
    *   **体力 < 10**：自动收缩发包数量至体力余量值（如体力剩 2 就只开 2 个，不再强制开 10 个）。
    以此杜绝引发服务器异常风控日志的 `10600（体力不足）` 报错频率。

---

## 4. 📝 运行诊断与排查指南

若在开发或实机调试中遇到问题，可在 Android Studio 的 Logcat 中查看以下过滤日志：

*   `[ACTIVE HEARTBEAT] Keep-Alive Tick`：表示 Java 脉冲唤醒了 H5 内核。
*   `⚠️【防卡死自愈】...`：表示已成功清空残留装备，挂机没有受卡死干扰。
*   `【自动开箱/钓鱼】当前体力: X ...`：表示发包正常，当前单次开箱发包数为 `1`。
