# 本地验证记录

2026-10-07，基于已发布的 BC03 1.7.2 0.1.5 APK。

- 16 项本地 JUnit 测试通过：协议地址/UUID 格式、原车事务读取，以及新增的状态缺失窗口、其他固件保留门槛、45 秒冷却、有效数据后重连、单调时钟起点和旧会话关闭不清除新冷却。
- API17 编译、D8 与合并通过。8189 类完全保留，4 类只有版本文字变化；6 个既有自行编写的 transport 类允许改动，新增 3 类（门槛、冷却和探测线程）。总类数 8202，无删除类、无编译桩混入。
- iAP2 握手、音视频、触摸、手机记忆、系统服务版本门槛、USB/NCM 类逻辑保留；classes.dex、classes2.dex、三份 ARMv7 native so 和其他资源/资产字节保留。
- ZIP CRC、4 字节对齐、DEX035、ELF32 ARMv7 和 Manifest 版本核对通过。versionCode45，显示 `Carplay-connect-0.1.6-beta-BC03-172-SPP-test`，minSdk17、targetSdk28。
- API17 签名检查通过：V1=true，V2/V3/V4=false，单一 RSA2048 签名；证书 SHA256 `88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323` 与既有包一致。签名工具对基线已有 META-INF 辅助元数据的保护范围发出提示，这些条目字节保持不变。

APK 大小：7,098,090 字节。

APK SHA256：`58e31391441d603fd81bc3a20719a07de6c66ac139a7d708371a374ab61bd46c`。

没有连接实体车机或 iPhone，没有设备 ADB 日志。不能据本地测试认定无线连接已修复，也没有验证现场 Android4.4 行为；图片中的运行时为 API25。8 秒无数据后的 socket 关闭、实际线程退出与端到端 iAP2/Wi-Fi 接续仍需现场验证。
