# 本地验证（2026-10-08）

27 项 JUnit4 测试通过：13 项释放/占用/取消/冷却边界测试、1 项开关默认值/进程恢复/其他设置保留测试、13 项 schema 格式与实际原厂 transaction 检查。

- 覆盖：关闭开关、有活动自有会话、没有占用均不发送断开；请求返回但仍连接不能继续；暂时断开后又连接不会提前通过；超时/调用异常后重试限流；取消前后不继续；冷却边界及已空闲通道恢复。
- OEM 1.3.6 / 1.7.9 输入文件实际读取到断开编号48；无参 void 签名、重复/缺失编号与不兼容签名检查通过。
- 原 DEX 8192 类，保留8177类原逻辑，3类仅更新显示版本，EnhancementPanel 更新版本及两处精确挂接。11个已有自编 helper 类允许改动；新增12个自编清理 helper/接口/闭包。原 iAP2、音视频、触摸、手机记忆、USB/NCM 主逻辑保留。
- APK 仅4个 payload 条目改变：AndroidManifest.xml、classes3.dex、about.html、releases.json。原始 classes1/2、三份 ARMv7 native 和其他资产逐字节不变。未新增权限或进程。
- DEX035、ZIP CRC/对齐、ELF32 ARM machine40、Manifest 最低API17/target28、versionCode46 校验通过。
- 纯 V1 API17 验签通过，RSA2048，证书SHA256 `88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323` 与旧包相同。继承原包 META-INF 非签名条目的 apksigner 提示，不影响验证。

APK SHA256：`99e46048bb01b08cd675d0b8fd72a89307744b9b22d4a6efbc5a0b301e939506`。尚无车机/ADB 现场验证；是否释放真实通道、原服务的 profile 联动和后续 CarPlay 握手结果需实测。不能将本地测试通过表述为无线已修复。
