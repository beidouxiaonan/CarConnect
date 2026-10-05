# CarConnect

CarConnect 是基于用户提供的 EasyPlay 0.2.7(36) APK 制作的 Android 4.4 车机测试补丁。
当前版本：**Carplay-connect-0.1.0-beta**，2026-10-05。

- 视频：合并已经解码的旧画面，积压时优先刷新解码器并请求关键帧，增加接收/解码/屏幕更新诊断。
- 触摸：保留此前车机反馈较流畅的有界触摸队列与移动事件合并。
- 设置：保留 30/60fps 请求上限，独立调整导航和媒体音量。
- 蓝牙：按实际安装的 BC03 接口签名和事务编号读取状态，不再因版本号不是 1.3.6 而直接跳过。
- 品牌：应用名 CarConnect，主页版本 Carplay-connect-0.1.0-beta；更新、版本、关于与下载入口指向维护者 GitHub 主页。

[维护者主页](https://github.com/beidouxiaonan) · [安装与使用](USAGE.zh-CN.md) · [版本修改说明](CHANGES.zh-CN.md) · [构建说明](BUILD.zh-CN.md) · [来源与许可](CREDITS.md)

## 当前验证状态

40 项本地 JVM 测试通过；DEX、API19 编译、v1/v2 签名、ZIP 对齐和 APK 内容对比通过。
保留稳定触摸补丁的 10 个类实现。CarConnect 本版尚未经车机实测，不能保证停帧已彻底消除。
原车状态读取也**不代表无线 CarPlay 数据通道已经适配**。BC03 1.7.9 必须在实际车机上核对。

## 文件

`CarConnect-0.1.0-beta-patch-source.zip` 为本次补丁源码、构建脚本和测试，不是原始 EasyPlay 全量源码。
其中包含 `easyplay-touchfix/` 与 `easyplay-stability3/`。原 APK、厂商 APK、认证资产、签名密钥、用户日志和本地工具不进入源码包。
测试 APK 与 SHA256 校验文件单独提供。生成 APK 的包名仍为 `com.shihab.diplay.legacy`，便于覆盖此前优化版并保留设置。
