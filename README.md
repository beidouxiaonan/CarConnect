# CarConnect

基于用户提供的 EasyPlay 0.2.7(36) APK 的车机测试补丁。当前版本：**Carplay-connect-0.1.5-beta-AV-test**，支持 Android 4.2～4.4。

## 0.1.5 音频与系统栏测试版

针对快刷新画面时音频断续，以及诺威达 K1201 / Android 4.4 手动启动后系统快捷栏不隐藏的反馈：补上媒体 PCM 预缓冲及断流补缓冲、适度提高音频工作/接收线程优先级、在手动进入和窗口焦点恢复时重新请求全屏。

**尚待实机验证，不承诺已解决厂商独立悬浮 Dock。** 媒体起播/恢复会有缓冲延迟；导航、Siri、通话等独立通道保持即时播放。USB/NCM、视频、触摸和原车蓝牙字节码沿用 0.1.4，也没有修复另一台 QuadCore-T3 的有线启动重启问题。

[下载 0.1.5 测试 APK 与补丁源码](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.5-beta-av-test) · [音频/快捷栏测试说明](AV-TEST.zh-CN.md) · [增量构建说明](AV-BUILD.zh-CN.md) · [验证记录](AV-VERIFICATION.zh-CN.md)

沿用 0.1.4 的原车无线流程：首次选择并记住 iPhone，以后启动优先恢复这台手机，不必每次重新选择。手机记录兼容已有版本；等待原车服务、暂时断连或其他手机接入时不会自动清除或替换记录。诊断移到独立入口，保留此前视频、触摸、30/60fps 设置和导航/媒体音量。

[简明操作手册](USAGE.zh-CN.md) · [修改说明](CHANGES.zh-CN.md) · [构建说明](BUILD.zh-CN.md) · [来源与许可](CREDITS.md) · [维护者主页](https://github.com/beidouxiaonan)

首次设置：原车蓝牙连接 iPhone 并开启热点 → CarConnect 设置开启原车蓝牙、选择并记住手机 → 手机允许 CarPlay。
以后上车：原车蓝牙、热点就绪，启动 CarConnect 等待自动连接。

原车模块本身未自动回连时，仍需从原车蓝牙页面连接已配对手机。本补丁恢复 CarConnect 的手机选择与无线会话，不代替原车配对系统。

0.1.4 的原车无线与手机记忆流程保持不变；该版此前完成 58 项本地测试。本次音频/全屏增量完成 17 项回归测试及 APK 校验，K1201 / Android 4.4 实机测试仍待完成。

[测试 APK 与补丁源码](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.5-beta-av-test)

源码包只包含自行编写的补丁、脚本、测试和说明，不是 EasyPlay 全量源码；不含输入 APK、原厂 APK、gocsdk、反编译原厂代码、认证文件、签名密钥或用户日志。许可证及第三方声明保留。