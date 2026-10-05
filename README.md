# CarConnect

基于用户提供的 EasyPlay 0.2.7(36) APK 制作的 Android 4.4 车机测试补丁。

当前新增测试版：**Carplay-connect-0.1.1-beta-OEM-test**。在用户反馈较流畅的 0.1.0 基础上，新增普通 APK 接入 BC03 1.7.9 / gocsdk 原车蓝牙的候选方案，默认关闭。
保留视频、触摸、30/60fps 设置及导航/媒体独立音量。名称仍为 CarConnect，网页入口指向维护者 GitHub 主页。

[维护者主页](https://github.com/beidouxiaonan) · [安装与使用](USAGE.zh-CN.md) · [修改说明](CHANGES.zh-CN.md) · [构建说明](BUILD.zh-CN.md) · [来源与许可](CREDITS.md)

47 项本地 JVM 测试及 API19 编译、DEX/签名/对齐/APK 内容检查通过。
**原车无线功能尚未做车机联调，不能当作已成功连接。** 端口权限、原车模块响应、iAP2 和 Wi-Fi 接续需实测。

[下载原车蓝牙测试版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.1-beta-oem-test) · [此前 0.1.0](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.0-beta)

源码附件只含补丁、脚本、测试和说明，不是 EasyPlay 全量源码，也不包含原厂 APK、gocsdk、反编译原厂代码、认证文件、签名密钥或用户日志。
