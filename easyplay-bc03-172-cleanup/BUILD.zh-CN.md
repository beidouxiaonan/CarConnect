# 0.1.8 BC03 1.7.2 恢复测试构建

在项目根目录运行 `easyplay-bc03-172-cleanup/build.ps1`，然后执行 `easyplay-bc03-172-cleanup/package_public.py`。PythonPath 可显式传入。依赖布局沿用 `.tooling/jdk25`、`.tooling/platform/android-4.2.2/android.jar`、`.tooling/build-tools/android-15`、`.tooling/jadx/lib/jadx-1.5.6-all.jar` 和 `.tooling/test-libs/{junit,hamcrest}.jar`。

固定输入 `artifacts/CarConnect-0.1.6-beta-BC03-1.7.2-SPP-test.apk`，SHA256 `58e31391441d603fd81bc3a20719a07de6c66ac139a7d708371a374ab61bd46c`。将私下取得的匹配 gocsdk 放入 `.private/vendor-analysis/gocsdk-native/gocsdk`；verify_native.py 在构建开始校验 SHA256 与无索引 VH 分支。缺文件或指纹不符直接失败，不跳过 native 依据。

实际 OEM schema 用例读取 `.private/vendor-analysis/{bc03,bc03-179,bc03-172}/input.apk`；缺少对应样本时这些用例明确跳过，不能将模拟用例宣称为原厂实测。本次本地有完整样本。公开源码不包含输入 APK、认证资产、签名密钥、原厂 APK/native、反编译代码或用户日志。

编译 API17 自编 transport、清理辅助与设置，执行60项JVM回归，D8 min-api17，精确修改版本及原有两处 UI 入口；stubs、JVM fake 和编译依赖不单独打入 APK。审计非清理类和原 OemSppGate canonical smali，检查 ZIP、原 payload、ARMv7、Manifest、对齐和签名。

输出 `artifacts/CarConnect-0.1.8-beta-BC03-1.7.2-SPP-native-recovery-test.apk` 及 SHA256，code50、minSdk17、targetSdk28，纯 V1。维护者使用原测试证书以支持覆盖安装；自行构建需自行配置签名，不同证书不能覆盖原包。源码包白名单仅收集指定自编 Java、脚本、测试和说明。

生成目录清理前验证绝对路径位于本项目 `.private/apk-analysis/easyplay/bc03-172-cleanup` 内，保留输入、源码与最终输出。验证结果见 [验证记录](VERIFICATION.zh-CN.md)。
