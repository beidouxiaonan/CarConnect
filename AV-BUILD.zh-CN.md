# 构建说明

本补丁针对已发布 0.1.4 APK，不是把上游 Android 项目重新编译成 APK。必须拥有合法取得的基线文件：

- `artifacts/CarConnect-0.1.4-beta-OEM-test-Android4.2.apk`
- SHA256：`dfccdeb5172d61840d5323a1857e3fe280d8b6c29ef30fe3ab6b937cb0871312`

构建固定校验基线哈希，抽取 classes3.dex，仅反汇编七个目标类，编译七个新增辅助类并合并。输入 APK、反汇编结果、签名密钥仅保存在本地 `.private`，不进入公开补丁源码包。

源码包保留仓库目录布局，包含 `easyplay-av5`、所需的 `easyplay-oem4/tools/DexTool.java`、二进制资源字符串池解析器 `easyplay-oem4/patch_brand.py`、Kotlin Function1 编译桩及出处说明。

本机构建入口为 PowerShell `easyplay-av5/build.ps1`。它使用仓库现有 `.tooling` 中的 JDK、Android API17 android.jar、D8、apksigner、zipalign、aapt、jadx/dexlib2 及 JUnit4/hamcrest。Python 路径可通过 `-PythonPath` 参数修改。

需要 `.private/kitkat-probe-debug.jks`（仅本地开发测试签名）才能覆盖原测试包。没有该密钥时必须修改签名设置，使用自己的密钥；新签名不能直接覆盖安装原签名 APK。不要把密钥放进源码仓库。

输出为独立测试文件 `artifacts/CarConnect-0.1.5-beta-AV-test-Android4.2-4.4.apk`，不会覆盖 0.1.4。构建校验包括：

1. API17 编译与 JVM 音频/全屏回归测试。
2. Smali 汇编、DEX 合并及逐类/逐方法规范化审计：仅允许既定七个原类中的目标方法与版本显示变化，保留其他基线代码。
3. APK ZIP CRC、DEX035、原生库 ARMv7 ELF32、全部资源/资产清单和变更白名单。
4. API17 最低版本 V1 签名、相同签名证书、ZIP 对齐和二进制 Manifest。

`aapt dump badging` 无法在缺少框架资源时解析基线自带的框架图标；此流程使用 `dump xmltree` 读取 Manifest。本次未修改该图标或资源表。

签名工具可能对基线自带的 META-INF 辅助元数据、LICENSE 或服务声明给出未保护条目的提示；本补丁保留这些基线条目，不把它们当作 V1 签名校验失败。API17 的 V1 验证必须通过。
