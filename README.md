# CarConnect

基于用户提供的 EasyPlay 0.2.7(36) APK 的车机测试补丁。当前版本：**Carplay-connect-0.1.4-beta-OEM-test**，支持 Android 4.2～4.4。

本版简化原车无线连接：首次选择并记住 iPhone，以后启动优先恢复这台手机，不必每次重新选择。手机记录兼容已有版本；等待原车服务、暂时断连或其他手机接入时不会自动清除或替换记录。诊断移到独立入口，保留此前视频、触摸、30/60fps 设置和导航/媒体音量。

[简明操作手册](USAGE.zh-CN.md) · [修改说明](CHANGES.zh-CN.md) · [构建说明](BUILD.zh-CN.md) · [来源与许可](CREDITS.md) · [维护者主页](https://github.com/beidouxiaonan)

首次设置：原车蓝牙连接 iPhone 并开启热点 → CarConnect 设置开启原车蓝牙、选择并记住手机 → 手机允许 CarPlay。
以后上车：原车蓝牙、热点就绪，启动 CarConnect 等待自动连接。

原车模块本身未自动回连时，仍需从原车蓝牙页面连接已配对手机。本补丁恢复 CarConnect 的手机选择与无线会话，不代替原车配对系统。

用户已反馈原车无线可连接；本次自动恢复改动尚待车机验证。58 项本地 JVM 测试通过，完成 API17 D8、DEX 差异、稳定视频/触摸、OEM 接口、签名、对齐及 APK 内容检查。

[测试 APK 与补丁源码](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.4-beta-oem-test)

源码包只包含自行编写的补丁、脚本、测试和说明，不是 EasyPlay 全量源码；不含输入 APK、原厂 APK、gocsdk、反编译原厂代码、认证文件、签名密钥或用户日志。许可证及第三方声明保留。