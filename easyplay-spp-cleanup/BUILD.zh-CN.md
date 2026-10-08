# 0.1.8 OEM SPP 恢复测试构建

执行 `easyplay-spp-cleanup/build.ps1`。需要本地 Python、JDK、API17 android.jar、D8/apksigner/zipalign、JADX smali 工具、JUnit，以及可覆盖已有 CarConnect 的原签名密钥。

固定输入 `artifacts/CarConnect-0.1.4-beta-OEM-test-Android4.2.apk`，SHA256：`dfccdeb5172d61840d5323a1857e3fe280d8b6c29ef30fe3ab6b937cb0871312`。在 `.private/vendor-analysis/gocsdk-native/gocsdk` 提供已授权的核对 daemon。核对 BC03 schema 的测试输入位于 `.private/vendor-analysis/bc03/input.apk` 和 `bc03-179/input.apk`；公开源码包不含这些 OEM 文件。

API17 编译，ARMv7 原库、DEX035、纯V1，versionCode49。不修改原 iAP2、音视频、触摸、手机存储、USB或二级DEX加载器。构建校验 native 路径证据、40项JVM测试、严格非清理类审计、Manifest/payload/ZIP CRC/签名和对齐；仅编译用 stubs 不打入 APK。

运行 `package_public.py` 生成自编源码白名单 ZIP 和四份说明。源码包不含基础APK、身份资产、签名密钥、OEM二进制/反编译代码、用户日志或工具安装包。其他证书不能覆盖已有安装，应保留原密钥。
