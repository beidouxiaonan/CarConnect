# BC03 1.7.2 · 0.1.10 构建说明

入口 `easyplay-bc03-172-guard/build.ps1`，Python 用 `-PythonPath` 指定。必须私下准备合法取得的精确输入：

- OEM 0.1.10 APK，SHA256 `c024cf21fb925fbf465ceeeb4031592559e0bbb5d4b738e1f0de1f8b702af8f7`，位于 artifacts 下对应文件名；[基线发布](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.10-beta-oem-spp-guard-test)。
- 1.7.2 0.1.8 APK，SHA256 `3824782100cf0d14545c300ba8c4ad749966454e33fd6628d3093ff060e7f2d5`；[固定音频/全屏参考](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.8-beta-bc03-172-spp-native-recovery-test)。
- `.private/vendor-analysis/{bc03-172,bc03-179,bc03}/input.apk`，以及 bc03-172/gocsdk；脚本固定核对三份样本及 daemon 哈希，再执行实际 schema 回归，不静默跳过缺失输入。

工具布局：`.tooling/jdk25`、`.tooling/platform/android-4.2.2/android.jar`、`.tooling/build-tools/android-15`、`.tooling/jadx/lib/jadx-1.5.6-all.jar`、`.tooling/test-libs/{junit,hamcrest}.jar`。公共依赖包括 easyplay-oem4 的 DexTool、patch_brand、OemProtocol、必要编译桩和测试 Context。

流程：核对输入及 daemon 映射 → API17 Java 编译 → 79 项连接/接口/保存回归 → 独立 classpath 的 17 项 AV 参考回归 → D8 min-api17 → 四个原类版本文字更新 → 合并自编接入类 → 精确导入固定 1.7.2 音频/全屏类和诊断方法 → canonical 审计 → 重打包、4字节对齐 → 纯 V1 签名与载荷检查。

输出 `artifacts/CarConnect-0.1.10-beta-BC03-1.7.2-SPP-guard-test-Android4.2-4.4.apk`，code53 / minSdk17 / targetSdk28。`package_public.py` 生成白名单源码 ZIP 与四份发布说明。

本地密钥 `.private/kitkat-probe-debug.jks` 不发布；自建证书不能覆盖维护者签名版本。原输入 APK、OEM 系统二进制、反编译结果、身份资产、密钥和用户日志不进入公开源码。测试 APK 延续用户已授权输入包的实验性离线身份资产。AV 参考源码只用于审查/测试；生产 AV 行为严格来自已发布参考 APK，不混入 JVM 假对象。
