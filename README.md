# CarConnect

基于用户提供的 EasyPlay 0.2.7(36) APK 的车机测试补丁。主分支汇集自行编写的补丁、回归测试和说明；各测试 APK 按车机服务和故障分别构建，版本号更高不代表包含其他测试分支的所有改动。

## 测试版选择

| 用途 | 发布版本 | 适用范围与状态 |
| --- | --- | --- |
| 停止清理失败后重复断开、底层恢复单独控制 | [0.1.10 OEM SPP 保护测试版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.10-beta-oem-spp-guard-test) | BC03 1.7.9 / 已核对 1.3.6；API17 起。40 项新增主机回归通过，**车机连接恢复尚待验证**。底层 VH 默认关闭，空闲状态稳定后正常连接。 |
| 原车无线、SPP 冷却等待、无首帧有限重连 | [0.1.9 OEM 测试版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.9-beta-oem-first-frame-diagnostics-test) | BC03 1.7.9；代码同时核对 1.3.6，需匹配 gocsdk。Android API17 起。**最新实车反馈仍有 SPP 清理超时，尚未解决持续占用。** |
| BC03 1.7.2 的 SPP 恢复与接入验证 | [0.1.8 BC03 1.7.2 测试版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.8-beta-bc03-172-spp-native-recovery-test) | 单独的 1.7.2 分支；不使用 OEM 1.7.9 包替代 |
| 快刷新画面时音频断续、手动启动后系统快捷栏显示 | [0.1.5 音频与全屏测试版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.5-beta-av-test) | 诺威达 K1201 / Android 4.4；实机效果待验证，厂商独立 Dock 接口未确认 |
| SD8227 安装后无法启动、旧系统 MultiDex 异常 | [0.1.7 SD8227 MultiDex 测试版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.7-sd8227-multidex-test) | 单独启动兼容分支；与原车无线适配分别验证 |

**合并源码不会改变已发布 APK。** OEM 0.1.9 沿用 OEM 0.1.8 基线，不包含 AV 0.1.5 的音频/全屏增量、BC03 1.7.2 接入策略或 SD8227 启动补丁。QuadCore-T3 的有线启动整机重启仍需独立诊断。

## OEM SPP 当前状态

[0.1.10 使用说明](OEM-SPP-GUARD-USAGE.zh-CN.md)修复清理失败后反复请求断开的路径：未完成标记跨进程保留，当前服务暂停自动重试；独立底层恢复默认关闭。通道确认空闲后沿用原连接。此改动不绕过持续占用/缓存门禁，实车尚未确认无线已恢复。

### 0.1.9 实车反馈

2026-10-09 的 BC03 1.7.9 实车反馈显示：原车 SppDisConnect 返回后，一次底层 VH 请求也未使 Binder 的 SPP 标志释放；状态观察未收到新的 SPP 广播，程序在发送 VF 之前停止。另有一次“连接已取消”，需要完整会话日志确定取消来源。不能据此认定占用来自某个后台应用，也不能认定缓存一定失效。

该失败发生在 iAP2 和无线首帧之前，60 秒无首帧重连对此无效。不修改缓存、不伪造释放广播、不跳过占用检查。诊断时先关闭自动连接，避免多轮记录混在一起；保留配对和手机选择。参见 [问题分析](OEM-RECOVERY-ANALYSIS.zh-CN.md) 和 [当前使用说明](OEM-RECOVERY-USAGE.zh-CN.md)。

## 操作和方案

首次：原车蓝牙连接 iPhone 并开启热点 → CarConnect 开启原车蓝牙路径、选择并保存手机 → 手机确认 CarPlay 授权。

以后上车：原车通话蓝牙和热点就绪，打开 CarConnect，等待自动连接。覆盖升级保留应用数据；正常重启无需重新选择 iPhone。原车模块尚未回连时，应用等待已保存手机，不代替原车配对系统。

[完整方案与适配文件清单](docs/OEM-WIRELESS-FEASIBILITY.zh-CN.md) · [离线图文说明](docs/OEM-WIRELESS-FEASIBILITY.zh-CN.html) · [原有简明手册](USAGE.zh-CN.md) · [修改说明](CHANGES.zh-CN.md) · [构建入口](BUILD.zh-CN.md)

主分支保留此前视频、触摸、30/60fps 设置和导航/媒体音量补丁。关于媒体预缓冲、线程优先级和标准全屏请求，见 [AV 测试说明](AV-TEST.zh-CN.md)、[AV 构建说明](AV-BUILD.zh-CN.md) 和 [AV 验证记录](AV-VERIFICATION.zh-CN.md)。

## 验证与来源

OEM 0.1.9 完成 51 项主机 JVM 回归和 API17 编译、DEX 差异、APK 载荷、对齐及签名检查。主机测试没有执行真实原车 Binder、PTY、蓝牙模块或 iPhone，不能替代实车验收。详见 [OEM 验证记录](OEM-RECOVERY-VERIFICATION.zh-CN.md)。

仓库不是 EasyPlay 全量源码。公开文件不含输入 APK、原厂 APK、gocsdk、原厂反编译代码、认证文件、签名密钥或用户日志；测试 APK 沿用用户已授权的原输入包实验性离线身份资产。保留 [来源与许可](CREDITS.md)。[维护者主页](https://github.com/beidouxiaonan)。
