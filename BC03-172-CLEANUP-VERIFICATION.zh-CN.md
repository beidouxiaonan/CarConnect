# 本地验证（2026-10-08）

47项JUnit4通过：13项SPP释放/超时/取消/冷却、1项开关存储、13项schema及实际OEM文件检查、9项原SPP gate/token回归、7项原协议回归、4项清理与VF探测衔接测试。

- 新衔接测试确认：VF窗口未结束时不查询/断开SPP；旧流关闭不放行新VF的清理；窗口结束后清理且保留原1.7.2探测边界；清理失败不创建VF请求。
- 三个实际OEM APK的SppDisConnect transaction48、无参void签名和手机布局已读取核对；未知/重复/缺失接口拒绝。
- 基线8202类，8186类canonical原样保留，包括OemSppGate两类；3类仅版本文字变化，EnhancementPanel含版本与两处挂接，12个已有自编helper类允许改动，新增14个自编清理helper/接口/闭包。原iAP2、音视频、触摸、手机记忆与USB/NCM主逻辑保留。
- APK只4个payload改变：Manifest、classes3.dex、about.html、releases.json。classes1/2、3个ARMv7 native及其他资产逐字节保留，未新增权限、launcher或进程。
- DEX035、ZIP CRC/对齐、ELF32 ARM machine40、API17/target28、versionCode47及纯V1验签通过。证书SHA256与0.1.6相同：`88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323`。

APK SHA256：`a1a2aae6a721a469c29011a1133696bb6c7170d1981984d2d9e5382eed2efc1e`。没有车机/ADB现场验证；不承诺清理或无线已成功，原服务状态上报、profile联动与iAP2后续结果需实测。
