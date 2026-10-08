# 0.1.8 OEM SPP 恢复验证（2026-10-08）

## 主机已验证

- 精确 gocsdk SHA256、VF/VG/VH 分发表对应和无索引 VH 管理通道循环已核对；不把这项静态检查记作实际蓝牙成功。
- **40 项 JVM 测试**：原13项释放/取消/所有权/冷却，新增8项native恢复与超时、2项完整无索引帧、3项VF窗口；13项AIDL schema及1项保存开关。覆盖空Binder后单次回退、有效Binder不回退、native写入不等于成功、权限错误、取消、迟到释放、状态抖动、全程超时和旧VF保护。
- 非清理类严格审计：8177类保持逻辑不变，4类只有版本文字或原有两处UI钩子，11个自编 transport 类允许替换；新增类仅限自编清理辅助范围。原iAP2/audio/video/touch/phone-memory/USB实现保留。
- 包名、权限、minSdk17、targetSdk28与原包一致，仅版本变化至code49/name0.1.8。同一RSA证书、纯V1，ZIP CRC、DEX035、ARMv7 ELF32、对齐及签名验证通过。
- 载荷仅修改classes3.dex、AndroidManifest.xml、about.html、releases.json；原resources、其他assets、classes1/2/4.dex与三个native库逐字节不变。四项基础包V1元数据警告保留，签名验证结果仍通过。
- 清理开关沿用原存储键，默认关闭，保存开关不影响音量、OEM选项或记住手机的值。

## 尚未验证

新包尚无该车机实测结果。JVM模拟状态和静态native分析不执行真实蓝牙模块、Binder或PTY，不证明C5一定出现。需要覆盖安装后验证占用场景、空闲场景、自动回连、取消和实车通话/音乐行为。

普通APK不能无条件释放所有系统SPP或阻止其他程序重新连接。后台重新占用、缓存不更新、串口拒绝或未知固件仍会明确失败；不绕过状态检查伪造成功。
