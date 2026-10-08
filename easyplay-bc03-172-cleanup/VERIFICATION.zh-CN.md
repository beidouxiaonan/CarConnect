# 本地验证（2026-10-08）

## 已通过

- **60项JUnit4回归**：21项释放/取消/冷却/native回退、1项开关保存、13项schema/实际样本检查、9项原SPP gate/token、7项原协议、7项清理与VF衔接、2项完整无索引帧。新增衔接覆盖旧VF到期后底层释放、新VF在Binder等待中出现时禁止native、native无释放时不发VF。
- 提供的1.7.2 gocsdk与原OEM样本SHA256相同；verify_native.py 精确核对 VF/VG/VH 分发表、无索引 VH 管理SPP循环及相同 disconnect_spp 目标。此项为静态验证，没有运行实际模块。
- 三个实际OEM APK的 transaction48 / 无参void断开、getter50及手机布局通过schema核对；未知/重复/缺失接口拒绝。
- 原8186类canonical保持一致，包括 OemSppGate 两类；4类仅版本或两处UI入口，12个原自编transport/helper类允许替换，新增15个自编清理相关类/闭包。原iAP2、音视频、触摸、手机记录与USB/NCM实现保留。
- 仅4个APK payload改变：Manifest、classes3.dex、about.html、releases.json。其他DEX、原resources、其他assets及3个ARMv7库逐字节保留；未新增权限、launcher或进程。
- DEX035、ZIP CRC/对齐、ELF32 ARM machine40、API17/target28、versionCode50与纯V1验签通过。V2/V3/V4关闭，同证书SHA256 `88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323`。保留原包4项META-INF条目未受V1保护的警告，整体验签通过。
- APK大小7,104,698字节，SHA256 `3824782100cf0d14545c300ba8c4ad749966454e33fd6628d3093ff060e7f2d5`。

## 尚待验证

新包没有车机或ADB现场验证。模拟回归与静态反汇编不证明native释放、profile联动或后续iAP2一定成功。覆盖安装后需验证占用/空闲、C3/C5、自动回连和原车通话/音乐。若C4后仍超时，复制最新完整诊断以区分状态缓存、重新占用、模块拒绝和权限问题。

不承诺所有固件一次解决，K2001N有线整机重启问题仍未解决。当前版本继续无线测试，SD8227启动与其他音频/全屏分支改动未合入。
