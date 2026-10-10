# CarConnect 0.1.12 · BC03统一适配 · 音画与方向盘优化

[正式版下载](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.12) · [完整功能与优化](FEATURES.zh-CN.md) · [操作手册](USAGE.zh-CN.md) · [适配条件与方案图](COMPATIBILITY.zh-CN.md) · [验证记录](RELEASE-VERIFICATION.zh-CN.md)

同一 APK 选择已核对的 BC03 **1.3.6 / 1.7.2 / 1.7.9** 配置，包含音频缓冲、独立导航/媒体音量、画面与触摸恢复、标准全屏、手机记忆、SPP 保护、有限首帧恢复及方向盘时间去重修正。

| 项目 | 正式版范围 |
|---|---|
| 安卓 | 最低 Android 4.2 / API17，含 4.4、4.4.2 分流；实际可用性仍依赖固件 |
| 架构 | ARMv7 32 位（armeabi-v7a），3 个 ELF32 ARM 原生库；不是 x86 通用包 |
| 签名 | 纯 V1，原包名和既有证书，versionCode55；覆盖安装保留手机、音量与映射 |
| 原车蓝牙 | 仅已经核对过的三种版本及对应 APK/gocsdk 指纹；不按版本区间自动放行 |
| 安装文件 | 只安装 CarConnect；原系统 BC03、Bluetooth.apk、gocsdk 保持原状，无需 Root |

安装包 `CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7.apk`，启动器名称 **CarConnect BC03**，设置标题 **CarConnect 0.1.12 · BC03/AV/Wheel**。版本名明确标出统一适配、音画/全屏、方向盘、安卓最低版本和 32 位架构。

## 功能与本版优化

- 无线/USB、原车/标准蓝牙入口、保存/更换 iPhone、自动恢复、热点信息和手填、三指下滑设置。
- 30/60fps 请求上限、有界触摸队列、连续移动合并、最新解码画面、视频停滞及首帧有限恢复。
- 导航/媒体分别 0～100% 音量，媒体预缓冲和断流补缓冲、实际容量保护、短尾部播放、音频线程调度。
- 手动进入、回前台、焦点和 CarPlay 变化时申请标准全屏；独立厂商 Dock 仍需验证。
- 已核对 BC03 统一分流、空闲直连、一次授权清理、未完成标记、失败暂停、独立底层恢复开关、冷却和阶段期限。
- 方向盘短按/长按映射、媒体信息与 Siri。本版修正固定/重复按键时间的长期过滤；API17～20 仅在 CarPlay 前台维护已有媒体入口，后台/失焦/断开时停止。
- 连接、视频、音频、系统栏、原车蓝牙/SPP 及自检诊断。

## 上车操作

首次原车蓝牙配对并连接 iPhone、开启热点，再在 CarConnect 保存手机并开启自动连接。以后原车蓝牙与热点就绪，打开应用等待，正常重启无需再选择。原车 HFP 回连仍由车机固件处理。升级请覆盖安装，不先卸载或清除数据。

底层 SPP 恢复默认关闭；空闲不清理，未确认清理暂停本轮，不要反复点连接。复制当前完整诊断再反馈。

## 验证与已知限制

**142 项主机回归通过**，API17 编译、纯 V1 签名、DEX035、ARMv7、对齐及完整载荷审计通过。本次新增方向盘效果仍需目标车机实测，正式发布状态不是所有固件已经验证的承诺。

**K2001N 有线启动整机重启尚未修复，使用无线；SD8227 专用启动/MultiDex 分支未合入。** 其他 BC03 文件/版本、厂商独立浮动 Dock、原车服务长期 SPP 占用仍需对应样本和实测。

[正式版构建](easyplay-release/BUILD.zh-CN.md) · [历史统一版 AV 核对](BC03-UNIFIED-AV-MERGE-VERIFICATION.zh-CN.md) · [历史 OEM 图文](docs/OEM-WIRELESS-FEASIBILITY.zh-CN.md) · [修改记录](CHANGES.zh-CN.md)

历史测试发布保留：[0.1.11 统一版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.11-beta-bc03-unified-test)、[0.1.5 AV](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.5-beta-av-test)、[SD8227 独立启动](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.7-sd8227-multidex-test)。

仓库包含自有补丁源码，不是 EasyPlay 全量源码；不含原厂 APK/gocsdk、反编译代码、身份资产、签名密钥和用户日志。APK 沿用用户授权输入已有的实验性离线身份资产；保留原许可及第三方声明。[来源与许可](CREDITS.md) · [维护者](https://github.com/beidouxiaonan)
