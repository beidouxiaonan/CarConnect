# 当前构建：0.1.11 BC03 统一适配测试

入口 `easyplay-bc03-unified/build.ps1`，随后 `package_public.py`。API17 / ARMv7 / 纯V1 / code54，三个实际蓝牙服务样本按版本与精确 APK/daemon 指纹分流。117项主机回归通过；实车待验证。详见 [构建](BC03-UNIFIED-BUILD.zh-CN.md)、[验证](BC03-UNIFIED-VERIFICATION.zh-CN.md)。

---

# 历史构建：0.1.10 BC03 1.7.2 SPP 保护与接入测试

入口 `easyplay-bc03-172-guard/build.ps1`，再运行 `package_public.py`。精确 OEM 0.1.10 基线加固定 1.7.2 0.1.8 音频/全屏参考；96项主机回归，API17 / ARMv7 / 纯V1 / code53。同证书覆盖保留手机。输入、系统样本及签名密钥均不发布。详见 [构建说明](BC03-172-SPP-GUARD-BUILD.zh-CN.md) 与 [验证](BC03-172-SPP-GUARD-VERIFICATION.zh-CN.md)。

---

# 历史构建：0.1.8 BC03 1.7.2 SPP 底层恢复测试

运行 `easyplay-bc03-172-cleanup/build.ps1`，然后 `easyplay-bc03-172-cleanup/package_public.py`。固定输入0.1.6 BC03 1.7.2 SPP APK，新增构建前精确 native 指纹与无索引 VH 检查，60项回归、API17、ARMv7、纯V1、code50。同签名可覆盖旧版，输入/签名密钥/原车样本不含在公开源码中。

详见 [构建说明](BC03-172-CLEANUP-BUILD.zh-CN.md) 与 [验证记录](BC03-172-CLEANUP-VERIFICATION.zh-CN.md)。以下保留历史说明，不表示当前构建版本：

---

# 0.1.5 增量构建

最新测试包先取得已发布 0.1.4 APK，再应用音频/全屏增量补丁。参见 [0.1.5 构建说明](AV-BUILD.zh-CN.md) 与本仓库 0.1.5 补丁源码压缩包。下面保留 0.1.4 基线的完整补丁构建说明。

# 原车蓝牙补丁构建说明

本次仍直接修改用户提供的 EasyPlay 0.2.7(36) APK，不是从完整 EasyPlay 源码构建，也没有替换本地 DiPlay 项目来冒充 APK 优化。

## 输入与工具

输入 APK 的 SHA256 必须为 `95c194c47fb4cab0fdc75d30b698b3aefeb037386cb5470290112393396a2b68`。
源码 ZIP 包含 `easyplay-touchfix/` 与 `easyplay-oem4/` 的新增源码、构建脚本、编译桩、测试、审计与文档；不含原 APK、认证资产、原厂 APK、gocsdk、真实手机数据或签名密钥。

工具布局与 0.1.0 相同：`.tooling/jdk25`、`.tooling/platform/android-4.4.2/android.jar`、`.tooling/build-tools/android-15`、`.tooling/jadx/lib/jadx-1.5.6-all.jar`、`.tooling/test-libs/{junit,hamcrest}.jar`。Python 可通过 build.ps1 参数指定。
另需本地测试证书。公共源码不提供维护者签名密钥，自建证书的 APK 不能直接覆盖维护者签名版本。

先运行 `easyplay-oem4/prepare_inputs.py <私下取得的输入APK>`，再运行 `easyplay-touchfix/build.ps1`，最后运行 `easyplay-oem4/build.ps1`。
如果本地有 0.1.0 构建 DEX，额外检查视频与触摸类保持一致；缺少该基线时明确跳过这一可选检查，其余检查仍执行。
私有 BC03 样本和反编译参考不存在时，对应集成用例及原厂参考审计明确跳过。协议和参数测试仍执行。

构建流程为 API19 Java 编译 → 58 项 JVM 测试 → D8（min-api 17）→ 精确 SMALI 修改 → 合并及原代码审计 → 重打包 → 对齐 → 签名 → APK 内容验证。
编译桩和 JVM 测试假对象不会打入 APK。

当前输出 `artifacts/CarConnect-0.1.4-beta-OEM-test-Android4.2.apk`，适用 Android 4.2～4.4，versionCode 41。
`patch_wireless.py` 在兼容补丁之后执行，调整原车选择、就绪、准备与主页面说明。`RememberedPhone` 沿用原偏好文件与键，不新建系统配对记录。`LegacyBootReceiver` 保持原实现。
新增 `LegacyActivity.patchUseRememberedPhone` 是补丁自己的 UI 入口；编译桩和 `LegacyPhoneSelector.Selection` / `Reason` 签名均按输入 DEX 核对，不把桩打进 APK。

## 原厂接入限制

只接入既有进程，不启动替代 daemon，不请求 Root、不调用 su、不 chmod、不改 system。
BC03 Binder 只查询，公开 SppConnect 的空实现不用于启动。
运行时先校验两个文件指纹，串口必须指向 `/dev/pts/数字`，以不创建、不截断的只写方式打开。
数据端口使用 Android API19 LocalSocket 的 FILESYSTEM 命名空间。错误、取消和超时关闭所属客户端。

原 APK 的原生库及实验性身份资产维持原内容，不能据此宣称新增 Apple/MFi 认证。测试 APK 内的原有实验身份可被提取；源码 ZIP 不含这些文件。
