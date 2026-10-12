# CarConnect：SD8227 / Android 4.4 兼容分支

## 最新：0.1.7 旧系统 MultiDex 加载兼容测试

2026-10-08 收到 0.1.6 启动排查包的完整记录：`SDK=17`，显示版本 `Android=4.4.2`，设备上报 `AC822X / ac8317 / autochipsac83xx`；错误是找不到三参数 `DexPathList.makeDexElements(ArrayList, File, ArrayList)`。原 APK 只保留 V19 三参数加载路径，主界面尚未创建。这份记录不能证明其芯片是 MTK8227，也未记录 USB 或蓝牙连接错误。

已新增 **[0.1.7 MultiDex 兼容测试包](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.7-sd8227-multidex-test)**，操作见 [STARTUP.zh-CN.md](STARTUP.zh-CN.md)：覆盖安装 → 点击“启动 CarConnect” → 确认主界面；仍失败则复制最新启动记录。无需 Root，不要卸载或清数据。

加载器识别运行时实际方法，支持两参数旧系统路径及三参数路径，并限定支持 `List/ArrayList` 和第四参数 `ClassLoader` 形式。所有二级 DEX 加载成功后才追加，保留原解压、锁、CRC 校验和单次重新提取重试。未知方法只记录签名，不尝试任意调用。

沿用原签名证书、包名及 V1 格式；APK 系统版本 0.1.7，versionCode 48，保留原手机选择记录和设置。业务主页面仍显示功能基础版本 0.1.4。19 项主机回归测试以及签名/载荷检查通过；**兼容包仍需车机验证，不能承诺完整 CarPlay 已可运行。** 不处理 K2001N 有线整机重启或 BC03 SPP 接入问题。

旧 [0.1.6 启动排查版](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.6-sd8227-startup-test) 保留，已由本次完整日志定位到加载阶段。

以下是旧 0.1.4 安装格式对照包说明。此前“尚未验证安装”的状态已由用户反馈更新为“可安装，启动失败待排查”。

分支：`codex/sd8227-kitkat`。基于已发布的 CarConnect 0.1.4 OEM-test，提供 **仅 V1 签名** 的安装格式对照包。用户已反馈该 V1 对照包能够安装，后续启动故障按上文处理。不能将该反馈推广至全部 SD8227 固件。

## 下载与安装

到 [测试发布页](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.4-sd8227-v1-test) 下载 `CarConnect-0.1.4-SD8227-V1-test.apk` 和相应 `.sha256` 文件。文件需完整下载并以 `.apk` 结尾。

1. 复制到车机内置存储，通过文件管理器安装；若从 U 盘安装失败，再试内置存储。
2. 已安装 CarConnect 的车机选择覆盖安装，**不要先卸载**。沿用 0.1.4 的包名、版本号及签名证书，覆盖成功会保留原应用数据，包括已选择的 iPhone。不能保留被卸载、清数据或重刷系统删除的记录。
3. 若仍报解析错误，有条件使用下方 ADB 采集脚本取得安装错误码。仅有中文弹窗无法区分签名、文件损坏、Manifest 解析和安装器问题。

## 改动范围

| 项目 | 已发布 0.1.4 | 本分支对照包 |
| --- | --- | --- |
| 签名 | V1 + V2 + V3 | 仅 V1，重新签名而非直接删除块 |
| APK 签名证书 | 原 CarConnect 调试证书 | 同一证书 |
| ZIP | 原包重新打包 | 经典 ZIP，无 ZIP64；重新对齐后签名 |
| 包名 / versionCode | `com.shihab.diplay.legacy` / 41 | 完全相同 |
| versionName / minSdk / targetSdk | 0.1.4 OEM-test / 17 / 28 | 完全相同 |
| ARM 库 | 3 个 `armeabi-v7a` 库 | 内容逐字节相同 |
| DEX / Manifest / resources / assets | 0.1.4 | 内容逐字节相同 |

主界面仍显示 `Carplay-connect-0.1.4-beta-OEM-test`，这是安装格式变体，不是新功能版本。无线连接、记住手机、触摸与视频优化沿用 0.1.4。主分支不作修改。

## 校正排查依据

- Android 4.4 不支持**只有 V2** 的 APK；但有效的 **V1+V2** 可以安装。AOSP 明确说明旧平台忽略 V2、验证 V1。因此“存在 V2 就拒绝 V1”不是原生 4.4 的规则。[AOSP 官方说明](https://source.android.com/docs/security/features/apksigning/v2)
- Android 按设备支持的 ABI 选择 APK 中相应的原生库，通用 APK 并不因同时存在 arm64 和 32 位库而必然被拒绝。本次原 APK 本来就只有 `armeabi-v7a`。[Android ABI 官方说明](https://developer.android.com/ndk/guides/abis)
- 原 0.1.4 已通过 apksigner 的 API 19 V1 验证，并有 ELF32 ARM 库。仅 V1 包用于排除设备安装器对签名块/ZIP 格式的特殊问题，不等同于证明原包缺 V1 或缺 ABI。
- `.so` 缺符号、依赖或链接器不兼容通常在加载库时表现为运行错误；要看 `dlopen` / `UnsatisfiedLinkError` 等日志。不能凭“解析包错误”认定是内核或新版链接器问题。
- K2101、SD8227 的型号名与界面版本号不足以证明安装器修改方式、实际 API、ABI 或内核。应读取设备属性及安装日志。本分支没有验证这些机型的固件差异。

## 仍失败时：取得具体错误码

准备官方 Android platform-tools，并在车机启用 USB 调试、确认电脑调试授权。不需要 Root。没有调试入口的车机暂时无法运行该脚本，应记录完整 APK 文件名、文件大小、车机系统信息与失败时机。

```powershell
powershell -ExecutionPolicy Bypass -File .\easyplay-sd8227\collect-install.ps1 -ApkPath .\CarConnect-0.1.4-SD8227-V1-test.apk -AdbPath C:\platform-tools\adb.exe
```

多台设备时再加 `-Serial 设备编号`。脚本通过 `adb install -r` 覆盖安装，采集真实系统版本、SDK、ABI、内核、安装器错误及相关 logcat；不卸载、不清数据、不清日志、不改系统。生成日志只在本地保存，检查隐私信息后再反馈，不要上传公开仓库。

`INSTALL_PARSE_FAILED_*` 指向具体解析环节；`INSTALL_FAILED_OLDER_SDK` 应核对真实 API；旧平台 ABI 失败可能显示 `INSTALL_FAILED_CPU_ABI_INCOMPATIBLE`，较新平台也可能显示 `INSTALL_FAILED_NO_MATCHING_ABIS`；证书不一致属于覆盖安装冲突，应先确认来源，不要为了测试直接卸载丢失记录。

## 构建与验证

执行 `build.ps1`。脚本固定核对输入 0.1.4 的 SHA256，重新打包、zipalign、用本地原证书重新签成 V1，强制关闭 V2/V3/V4。验证 API 17、19、24 签名、证书一致性、ZIP CRC、对齐、所有非签名内容逐字节相同、DEX 035、ELF32 ARM、无新签名块和反剥离标记。

工具路径可通过 `-PythonPath` / `-ToolingRoot` 指定。默认构建依赖本地 `.tooling` 与 `.private`。源码包不含基础 APK、签名密钥、原包身份资产、车机日志或 OEM 系统二进制；需要自行提供已授权基础包和原签名密钥。其他签名密钥无法覆盖现有 CarConnect。

本分支没有重新编译原生库或修改业务代码，所以本次只报告与安装格式有关的验证，不能把主机验证称为 SD8227 实机验证。原项目与第三方许可说明见随附 `LICENSE`、`CREDITS.md`。
