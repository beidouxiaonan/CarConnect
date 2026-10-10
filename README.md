# CarConnect

基于用户提供的 EasyPlay 0.2.7(36) APK 的车机测试补丁。主分支现已汇集 **0.1.11 BC03 统一适配版**的源码、测试和完整功能文档，包含已核对的 BC03 1.7.2 / 1.7.9 / 1.3.6 配置，以及 0.1.5 的音频与系统栏优化。

[下载 0.1.11 统一测试 APK](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.11-beta-bc03-unified-test) · [完整功能与优化清单](BC03-UNIFIED-FEATURES.zh-CN.md) · [操作手册](BC03-UNIFIED-USAGE.zh-CN.md) · [音频 / 系统栏合并核对](BC03-UNIFIED-AV-MERGE-VERIFICATION.zh-CN.md)

## 安装与适配范围

| 项目 | 当前统一 APK |
|---|---|
| 安卓最低版本 | Android 4.2 / API17，包含 Android 4.4 / 4.4.2 API19 运行分流；不是所有固件的实测保证 |
| CPU / 系统 | 32 位 ARMv7（armeabi-v7a）；3 个原生库均为 ELF32 ARM，不是 x86 32 位通用包 |
| 签名 | 纯 V1，按 API17 校验通过；同包名及测试证书可覆盖保留数据 |
| BC03 无线 | 仅已核对的 1.7.2 / 1.7.9 / 1.3.6 APK、gocsdk 与接口配置；其他版本需实际样本 |
| 安装文件 | 用户只安装 CarConnect APK；原车 BC03 服务及 gocsdk 使用原系统文件，无需 Root |
| 仍需单独处理 | SD8227 启动 / MultiDex、K2001N 有线整机重启、厂商独立悬浮 Dock |

安装版本名为 `Carplay-connect-0.1.11-beta-BC03-unified-test`，versionCode54，包名 `com.shihab.diplay.legacy`。安装条件符合不等于每台车机的启动、音视频、蓝牙固件均已验证。方向盘上一曲 / 下一曲在持续使用后失效的新反馈正在排查，0.1.11 尚未包含针对该反馈的新修复。

## 功能与优化

- 有线 / 无线模式、原车 / 标准蓝牙路径、记住及更换 iPhone、自动连接与暂停、热点信息和手填、三指下滑设置。
- 30 / 60fps 请求上限、独立触摸发送队列、移动事件合并、最新解码画面及有限恢复、Siri / 麦克风和方向盘学习。
- 导航与媒体分别 0～100% 音量，媒体实际预缓冲 / 断流补缓冲、容量保护、音频线程调度和即时语音通道。
- 手动启动、前台恢复、窗口获得焦点和进入 CarPlay 时申请标准全屏；厂商浮动 Dock 仍需验证。
- SPP 空闲直连、授权一次清理、持久化未完成标记、失败暂停、独立底层恢复开关、VF 冷却、阶段期限和 socket flush 修正。
- 无首帧有限自动恢复，以及连接、原车蓝牙、视频、音频、系统栏和车机自检诊断。

详见[完整清单](BC03-UNIFIED-FEATURES.zh-CN.md)中的选项默认值、限制和流程图。

## 每天上车

首次：原车蓝牙配对并连接 iPhone、开启车机热点 → CarConnect 开启原车路径，选择并保存手机 → 手机允许 CarPlay。以后原车蓝牙和热点就绪，打开应用等待自动连接；正常重启不必重新选择手机。原车 HFP 回连由固件管理。

底层 SPP 恢复默认关闭。清理未确认时暂停本轮重试，不要连续切换或反复发送请求；空闲时不清理。复制当前完整诊断再反馈。

## 构建、验证与历史版本

117 项主机回归及 API17 编译、签名、DEX / 载荷检查通过；0.1.5 与 0.1.11 的 10 个相关完整 AV 类 / 调用入口已直接核对一致。合并主分支只整合源码和文档，不重新签名或替换已发布的 APK。

[当前构建入口](BUILD.zh-CN.md) · [统一方案分析](BC03-UNIFIED-ANALYSIS.zh-CN.md) · [验证记录](BC03-UNIFIED-VERIFICATION.zh-CN.md) · [历史 OEM 可行性图文](docs/OEM-WIRELESS-FEASIBILITY.zh-CN.md) · [历史离线图文](docs/OEM-WIRELESS-FEASIBILITY.zh-CN.html) · [修改记录](CHANGES.zh-CN.md)

| 独立历史测试包 | 用途与边界 |
|---|---|
| [0.1.9 OEM](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.9-beta-oem-first-frame-diagnostics-test) | 历史 SPP / 首帧诊断；旧包不包含统一包后来的组合改动 |
| [0.1.8 BC03 1.7.2](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.8-beta-bc03-172-spp-native-recovery-test) | 历史 1.7.2 接入；截图 EBADF 的分析见统一方案 |
| [0.1.5 AV](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.5-beta-av-test) | 音频与标准全屏优化已经包含在当前统一包 |
| [0.1.7 SD8227 MultiDex](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.7-sd8227-multidex-test) | 单独的启动兼容测试；尚未并入统一 APK |

仓库不是 EasyPlay 全量源码。公开源码不含原厂 APK / gocsdk / 反编译代码、身份资产、密钥或用户日志；测试 APK 保留用户授权输入的原有实验性离线身份资产。[来源与许可](CREDITS.md) · [维护者主页](https://github.com/beidouxiaonan)。
