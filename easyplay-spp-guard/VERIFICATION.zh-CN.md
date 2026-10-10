# OEM 0.1.10 验证记录 · 2026-10-10

- **40 项新增 JVM 回归通过**：24 项清理保护/稳定空闲/冷却/取消，8 项实际偏好访问与保存结果，8 项服务暂停归属及同步会话替换。覆盖旧清理开关开启时 native 默认关闭、请求前写标记、未确认后的多次重试不重发、新清理对象仍遵守标记、迟到释放、状态抖动、写盘失败、用户 opt-out、其他失败与有线继续原重试。
- API17 编译与 D8 通过。精确基线 OEM 0.1.9：**8197 个原类 canonical 逻辑不变**，4 类仅版本/两个调用点改变，15 个既有自编 transport 家族类允许替换，新增9类均在自编白名单内。原首帧恢复、iAP2、手机保存、Binder schema、native 命令帧、音频、视频、触摸及 USB 保持。
- ZIP CRC、DEX035/引用上限、ARMv7 ELF32、Manifest minSdk17/target28/code52、4 字节对齐和纯 V1 签名通过。证书 SHA256：`88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323`，与原版本相同。
- APK 仅变化 classes3.dex、AndroidManifest.xml、about.html、releases.json；其他 DEX、资源、原三个 native 库及其余资产逐字节保持。基线四项 META-INF 元数据未签名警告保留。
- APK SHA256：`c024cf21fb925fbf465ceeeb4031592559e0bbb5d4b738e1f0de1f8b702af8f7`。

**目标车机尚未验证这版。** 模拟环境不执行真实 Binder、PTY、模块或 iPhone；不能证明原车缓存会释放或无线连接已经恢复。实车重点是默认不发 VH，清理失败后本轮重试暂停，重开应用不会继续清理，空闲恢复后可以正常接入，手机记录不丢失。需要确认原车端广播、释放状态与 iAP2/首帧。
