# 构建

在项目根目录运行 `easyplay-spp-cleanup/build.ps1`。Windows PowerShell、JDK25、Python3、Android API17 android.jar、D8/zipalign/apksigner、jadx dexlib2 工具包和 JUnit4/hamcrest 路径沿用 `.tooling`；可通过 PythonPath 参数指定 Python。

输入为 `artifacts/CarConnect-0.1.4-beta-OEM-test-Android4.2.apk`，SHA256 `dfccdeb5172d61840d5323a1857e3fe280d8b6c29ef30fe3ab6b937cb0871312`。补丁包不包含原 APK、实验性离线身份资产、原厂 APK/native、反编译原厂源码或签名密钥。

增量构建只编译自行编写的 SPP 清理、设置和 schema helper，再加入两处设置入口调用和版本文字。编译用 stubs 和 JVM Context fake 不打入 APK。自动核对非清理类的 canonical smali、原包 payload、DEX035、ARMv7 ELF32、API17 签名、ZIP 对齐/CRC 和 Manifest。

输出 `artifacts/CarConnect-0.1.7-beta-OEM-SPP-cleanup-test-Android4.2-4.4.apk` 及 `.apk.sha256`。本地已有测试 keystore 用于同签名覆盖；自行构建应配置自己的密钥，签名不同不能覆盖已安装的原测试包。

脚本仅清除经绝对路径检查、位于本项目 `.private/apk-analysis/easyplay/spp-cleanup` 中的三个生成子目录，保留输入、源码和最终 APK。签名始终先 zipalign，纯 V1。
