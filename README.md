# CarConnect 0.1.18 SD8227 · MultiDex + BC03统一适配 + 30秒首帧恢复 + 全部日志

本版在精确 0.1.17 APK 功能基线上集成已发布 0.1.7 SD8227 启动修复。分支 `codex/sd8227-kitkat`；新发布标签 `v0.1.18-sd8227-unified-test`。原 0.1.7 标签及发布资产保留。

[0.1.18 SD8227 全功能测试版下载](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.18-sd8227-unified-test) · [完整功能](FEATURES.zh-CN.md) · [启动说明](STARTUP.zh-CN.md) · [验证记录](VERIFICATION.zh-CN.md)

## 安装与启动

覆盖安装 `CarConnect-0.1.18-SD8227-Unified-BC03-FirstFrame30s-AllLogs-Android4.2plus-ARMv7.apk`。同包名、原证书、纯 V1，versionCode61，可覆盖旧 SD8227 版本以及 0.1.17，保留手机、自动连接、热点、音量和方向盘映射；无需 ROOT，安装本身不要求整机重启。

第一次打开先进入启动检查页，点击“启动 CarConnect”。成功进入原主界面后，后续正常打开可自动进入；失败、启动中断或主动诊断不会循环自动启动。旧快捷方式不正常时，从应用列表重新创建快捷方式。

旧设备实际 SDK=17、界面 Android 版本可能显示 4.4.2。兼容依据是实际运行时/ABI，不单凭俗称 SD8227 判断芯片。加载器保留原 MultiDex 提取、锁、CRC 与 IOException 单次重提取，支持已限定的两参数/三参数/List 与 ClassLoader 变体，失败不追加半成品或调用未知签名。

日志分享 provider 移入主 DEX，保持原权限和 URI 校验；加载失败时不再依赖它尚未载入的二级 DEX。原 onCreate/onStart/onResume 业务方法都保留并调用一次，音频、全屏与方向盘生命周期钩子保留。

## 完整功能

| 类别 | 集成内容 |
|---|---|
| 启动 | API17 最小 Application、运行时 MultiDex 工厂适配、软件渲染检查入口、失败/中断保护、启动与崩溃持久记录 |
| 连接 | 无线/USB、原车/标准蓝牙路径、保存/更换 iPhone、自动连接、热点读取与手填、返回 CarPlay、三指下滑设置 |
| BC03 | 已核对 1.7.2 / 1.7.9 / 1.3.6 原样本及 AT+ 样本；版本、APK/gocsdk 指纹及 Binder 手机结构联合匹配 |
| 首帧 | 前台且有效 Surface 时连续 30 秒无首帧触发有限恢复，最多两次；首帧出现停止，后台/Surface 丢失重新计时 |
| 会话/SPP | 空闲直连、占用保护、未确认记录、原冷却和阶段期限、默认关闭的底层 VH 恢复、一次手动恢复进度与互斥 |
| 退出/休眠 | socket 双向 shutdown 与关闭后数据隔离；默认关闭的灭屏退出/亮屏只读复查，不在唤醒后补发旧断开请求 |
| 画面/触摸 | H.264、30/60fps 请求选项、刷新率显示、有限触摸队列、移动合并、最新解码画面及停滞恢复 |
| 音频/系统栏 | 媒体预缓冲/断流恢复、独立导航与媒体音量、音频线程调度、进入/返回前台/焦点变化时申请标准全屏 |
| 按键/语音 | 方向盘短按/长按学习与映射、上一曲/下一曲等事件去重修正、旧安卓媒体入口维护、Siri、麦克风和通话入口 |
| 诊断/导出 | 连接/SPP/自检/音频/视频/系统栏报告、全部应用日志导出、绝对文件夹配置、不覆盖旧报告；新增启动/MultiDex/崩溃记录章节 |

仅查询系统信息的原分支 `easyplay-sd8227/SysInfo-1.0.apk` 保持原文件，是可选独立工具，不替代 CarConnect 核心安装包。

全部日志按钮在连接诊断与无线设置中。旧安卓默认 Download/CarConnect，可指定绝对文件夹；后台 UTF-8 写入并显示完成路径。主界面打不开时仍可在启动检查页复制启动记录。历史只含本应用已经保存的有界记录，不包含原车完整 HCI/其他进程日志。

## 适配条件与限制

Android 4.2+/API17，ARMv7 32 位；不是 x86/纯 64 位通用包。标准蓝牙和原车蓝牙分别走原流程；BC03 功能仍依赖已核对服务、gocsdk、socket 和虚拟串口存在/可访问。适配四个精确样本，不表示支持 1.7.2～1.7.9 所有固件。

首帧 30 秒只是触发恢复的期限，随后仍执行退出、退避及 SPP/VF 冷却。仍未保证原车长期 SPP 占用、缺少回执、休眠回连或 K2001N 有线整机重启已解决；独立厂商浮动 Dock 也仍需验证。本包尚无新的目标车机实测结果，发布为 SD8227 测试版，0.1.17 主分支正式发布保持原状。

## 构建与验证

根目录运行 `./easyplay-sd8227-unified/build-unified.ps1`。需要精确 0.1.17 APK（SHA256 `4e65747ca00cc7e876e4552c2fc161af488449bd2cec4cbc3e9946572eeafa39`）、私有签名密钥与工具链，不随公开源码提供。

本次 16 项 JVM 加载器/启动恢复与 5 项 Manifest 测试通过；基线原有 79 项主机回归已验证。独立全 DEX 审计确认 0.1.17 全部既有类保留，10003 类 canonical 完全不变，11 类仅允许的启动包装、显示版本或日志合并变化，新增 12 个主 DEX 启动类。主 DEX provider 独立依赖审计通过，现有业务 onResume 等方法逐方法一致。三份 ARMv7 原生库、classes2.dex、核心资源与其他 assets 保持原字节，仅更新关于/版本元数据。

API17 编译，DEX035，API17/19/24 纯 V1 原证书、经典 ZIP CRC/对齐检查通过。主机测试不替代实际 Dalvik、蓝牙模块、iPhone、解码器与长期播放实测。

---

## 主分支功能基线与历史发布

# CarConnect 0.1.17 · BC03 统一适配 · 30 秒首帧等待与全部日志

当前 main 整合兼容已核对 **BC03 1.7.2 / 1.7.9 / 1.3.6（含 AT+ 样本）** 的最新恢复与诊断代码，并将原车无线控制启动后的首帧等待从 60 秒改为 **30 秒**。前台且 Surface 有效、连续无首帧才触发恢复，最多两次；首帧出现立即停止，仍使用原会话退出与退避。

[0.1.17 正式版下载](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.17) · [当前版本功能、适配和构建](easyplay-integration17/README.zh-CN.md) · [完整功能清单](FEATURES.zh-CN.md) · [最新验证记录](INTEGRATION-0.1.17-VERIFICATION.zh-CN.md)

保留手机记忆、SPP 保护、会话双向关闭与旧数据隔离、一次恢复进度与互斥、可关闭的休眠只读复查、全部日志及指定文件夹导出，以及音频、全屏、视频/触摸、方向盘、Siri/麦克风和 USB。目标为 Android 4.2+/API17、ARMv7 32 位，无需 ROOT；同包名/证书，versionCode60，可覆盖安装。

**79 项主机回归与 API17/DEX035/ARMv7/V1/载荷审计通过，尚不能证明实车长期 SPP 占用和休眠回连已彻底解决。** 首帧 30 秒是恢复触发期限，不是保证 30 秒内显示画面。0.1.17 已于 2026-10-12 发布为最新正式 Release，包含 APK、源码、校验文件、适配说明与验证记录；六个附件的服务器 SHA256 和大小均已核对，历史 0.1.12 发布资产保留。

---

## 上一正式发布 0.1.12 与历史说明

# CarConnect 0.1.12 · BC03统一适配 · 音画与方向盘优化

[0.1.12 历史正式版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.12) · [完整功能与优化](FEATURES.zh-CN.md) · [操作手册](USAGE.zh-CN.md) · [适配条件与方案图](COMPATIBILITY.zh-CN.md) · [验证记录](RELEASE-VERIFICATION.zh-CN.md)

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
