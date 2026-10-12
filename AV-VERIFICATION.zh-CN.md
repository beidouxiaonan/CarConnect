# 本次构建检查

日期：2026-10-07。

- APK：`CarConnect-0.1.5-beta-AV-test-Android4.2-4.4.apk`
- 大小：7,096,028 字节。
- SHA256：`46b756fe289e47b3d41bec1a4d79da3c4fe752f3ece6a797ffca812d7216d1e8`
- 包名：`com.shihab.diplay.legacy`；versionCode 42；versionName `Carplay-connect-0.1.5-beta-AV-test`。
- minSdk 17（Android 4.2 起）；targetSdk 28。新增辅助代码针对 Android API17 编译。
- 纯 V1 签名通过 API17 验证；证书 SHA256 与 0.1.4 相同：`88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323`。
- ZIP 对齐、全部条目 CRC、DEX035、三个原生库 ARMv7 ELF32 验证通过。
- 三个原生库、classes.dex、classes2.dex、其余资产和资源的内容与 0.1.4 完全一致。
- classes3.dex 逐类/逐方法审计：8185 个原类不变，7 个原类仅改目标方法或版本文本，新增 7 个辅助类。视频解码恢复、触摸、USB/NCM 和原车蓝牙代码保持不变。
- 非签名 APK 条目仅改变 `classes3.dex`、`AndroidManifest.xml`、about.html、releases.json；Manifest 仅改变版本名与版本号，权限及组件不变。
- 17 项 JVM 测试通过：真实启动阈值、实际小容量、容量未知回退、短媒体片段、断流补缓冲、不丢弃待播音频、停止后不复播、厂商 API 异常、无符号播放指针回绕、优先级拒绝、手动启动焦点、进入 CarPlay、离开应用取消、有限全屏重试、API17 与 E03 例外。

未完成：K1201 / Android 4.4 实机安装、快刷新场景音频对比、厂商快捷栏隐藏验证及实际播放延迟测量。JVM 替身不执行 Android 音频驱动、厂商窗口管理器或原生 CarPlay 协议栈，不能据此声称这两个现象已在车机上解决。

K1201 的系统快捷栏尚未确认由 SystemUI 还是独立应用绘制；本包只使用标准窗口接口。也未改变会触发另一台 QuadCore-T3 整机重启的有线启动逻辑。
