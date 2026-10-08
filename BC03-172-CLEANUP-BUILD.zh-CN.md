# 构建

在项目根目录运行 `easyplay-bc03-172-cleanup/build.ps1`。JDK25、Python3、Android API17、D8/zipalign/apksigner、jadx和JUnit4/hamcrest路径沿用 `.tooling`；PythonPath可显式传入。

精确输入 `artifacts/CarConnect-0.1.6-beta-BC03-1.7.2-SPP-test.apk`，SHA256 `58e31391441d603fd81bc3a20719a07de6c66ac139a7d708371a374ab61bd46c`。补丁源码不含输入APK、身份资产、签名密钥、原厂APK/native/反编译代码或用户日志。

编译自行编写的清理、设置、schema和transport helper，保留原OemSppGate，加入两处精确设置入口和版本文字。stubs、JVM Context fake和编译用OemProtocol均不作为依赖打包。构建核对非清理类canonical smali、原包payload、ZIP CRC/对齐、DEX035、ARMv7 ELF32、API17签名和Manifest。

输出 `artifacts/CarConnect-0.1.7-beta-BC03-1.7.2-SPP-cleanup-test.apk` 与SHA256文件。本地已有测试keystore用于覆盖安装；自行构建应配置自己的签名，不同签名不能覆盖旧包。签名为纯V1，先zipalign。

仅清理经绝对路径验证、位于本项目 `.private/apk-analysis/easyplay/bc03-172-cleanup` 中的生成子目录，保留输入、源码和最终输出。
