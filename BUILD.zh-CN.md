# 补丁构建与内容说明

源码包只包含新增 Java 辅助类、SMALI 修改脚本、审计工具、测试和文档，不包含原 APK 的完整源码。
原 APK 并非本仓库原创；不能把解包重签名称为从完整源码编译 EasyPlay。

## 本地输入与工具

- 私下取得与本次相同的 EasyPlay 输入 APK，SHA256：
  `95c194c47fb4cab0fdc75d30b698b3aefeb037386cb5470290112393396a2b68`。
- JDK25，位于 `.tooling/jdk25`；Python3.13，build.ps1 可传 -PythonPath。
- Android API19 的 android.jar：`.tooling/platform/android-4.4.2/android.jar`。
- Android build-tools：`.tooling/build-tools/android-15`，含 D8、aapt、zipalign、apksigner。
- JADX1.5.6完整jar：`.tooling/jadx/lib/jadx-1.5.6-all.jar`，使用其中 smali/dexlib2 的 Java API。
- JUnit4.13.2与Hamcrest：`.tooling/test-libs/junit.jar`、hamcrest.jar。
- 本地签名密钥：`.private/kitkat-probe-debug.jks`，构建脚本为此前优化版测试证书路径。
  公共源码包不含该密钥。其他开发者需自建自己的测试证书，不能覆盖本作者签名版本。

## 步骤

在源码包根目录，用 prepare_inputs.py 将 APK 放入私有目录并提取3个DEX。
先运行 easyplay-touchfix/build.ps1 生成稳定触摸补丁，再运行 easyplay-stability3/build.ps1。
构建过程：API19 Java编译 -> JVM测试 -> D8 -> 原类disassemble -> 精确SMALI修改 -> assemble/merge -> DEX审计 -> APK重打包 -> 对齐 -> 签名 -> 内容核对。

BC03运行时查询按安装包核对，无须预先打入厂商代码。本地完整测试使用私有 BC03 样本；
未提供 `.private/vendor-analysis/bc03/input.apk` 时对应1个集成用例跳过，其余用例仍执行。
verify_vendor_schema.py 的参考Java源静态审计仅在私有反编译样本存在时执行，缺少样本会明确标为跳过。

本版 tests/stubs 只供 JVM 与编译使用，不打进 APK；原生库和认证资产来自原输入包并保持相同内容。
公开的源码ZIP不包含输入包、认证文件、原厂APK、原厂反编译代码、Android签名密钥、真实手机地址和日志。

## 认证资产与分发

原输入 APK 内含实验性离线附件身份；测试 APK 沿用这些资产。它们能从 APK 提取，
不应被视为新的 Apple/MFi 认证或作者私有身份。源代码ZIP排除认证资产及签名密钥。
上游对实验身份来源与限制有说明。未来iOS与车机兼容性仍需测试。
