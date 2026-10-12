# 0.1.18 SD8227 整合验证（2026-10-13）

- 功能输入：精确已发布 0.1.17 APK，SHA256 `4e65747ca00cc7e876e4552c2fc161af488449bd2cec4cbc3e9946572eeafa39`。
- 启动输入：SD8227 分支 0.1.7 自有源码与现有后续分支历史；LegacyDexInstaller 原逻辑保留，新增当前生命周期方法和主 DEX provider 处理。
- 16 项 JVM 测试与 5 项 Manifest 测试通过。基线 0.1.17 的 79 项回归记录保留，未将 JVM fake 称作新车机实测。
- 10003 个既有类 canonical 不变，11 类明确变更，全部旧类保留，新增 12 个启动类。原 onCreate/onStart/onResume 业务主体逐方法不变，原全屏/方向盘/首帧钩子保留。
- MultiDex 只替换一个 installer 调用；原提取、锁、CRC 与重试不变。Standalone OemDiagnosticProvider 完整迁移到主 DEX，无二级业务依赖，二级副本删除以避免重复类。
- classes2.dex、三份原 ARMv7 ELF32 库、核心资源和其他 assets 原字节保留；仅主/第三 DEX、Launcher/Application/版本 Manifest、about/releases 元数据允许变更。
- API17 编译，DEX035，minSdk17、targetSdk28、versionCode61、同包名/证书。API17/19/24 V1 验证、经典 ZIP CRC、无新增签名块及 ZIP 对齐通过。
- 启动记录加入全部日志导出；继续后台写入、脱敏和有界历史策略。
- 新包 SHA256：`84c561306b862079da12a6c3a9831c7af30d590b90098695f5f22d73d77deb5f`。

所有主机验证不能保证目标设备的全部框架签名、原车 SPP 回执、USB 供电或音视频长期负载。仍需实际打开、无线连接、首帧、播放/切歌、休眠/唤醒和日志导出实测。旧 0.1.7 标签/资产不修改，新包作为同一 SD8227 分支的新测试发布。
