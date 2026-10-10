# OEM 0.1.10 构建说明

执行 `easyplay-spp-guard/build.ps1`。固定输入为已发布 OEM 0.1.9 APK，SHA256：`cddbcfc1ec88d201dd0e42505caea7c4ccedd90edae2fd43c9e859c922351452`。先核对输入哈希，再取 classes3.dex；仅替换自行编写 OemTransport/SppRelease，增加三个自编辅助类及其嵌套类，精确修改两个调用点与版本文字。

工具布局：`.tooling/jdk25`、`.tooling/platform/android-4.2.2/android.jar`、`.tooling/build-tools/android-15`、`.tooling/jadx/lib/jadx-1.5.6-all.jar`、`.tooling/test-libs/{junit,hamcrest}.jar`。Python 通过脚本参数指定。本地编译桩和 JVM 假对象不合并进 APK。

API17 Java 编译 → 40 项新增 JVM 回归 → D8 min-api17 → 四个目标类反汇编/精确调用点补丁 → 合并 → canonical DEX 审计 → 原 ZIP 载荷重打包 → 4 字节对齐 → 原测试证书纯 V1 签名 → 载荷/签名/Manifest 检查。

输出 `artifacts/CarConnect-0.1.10-beta-OEM-SPP-guard-test-Android4.2-4.4.apk`，code52，minSdk17，targetSdk28。执行 `easyplay-spp-guard/package_public.py` 生成白名单源码包及发布说明。

构建须私下准备输入 APK、签名密钥和工具。公开仓库不含输入 APK、身份资产、OEM APK/gocsdk、反编译原厂代码、密钥或用户日志。自建证书不能覆盖维护者签名版本；不能把自建证书等同于 Apple 认证。原 APK 内实验身份资产保持输入内容，用户已授权测试 APK 发布。
