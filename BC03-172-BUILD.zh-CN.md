# 构建 BC03 1.7.2 增量测试包

以固定 `CarConnect-0.1.5-beta-AV-test-Android4.2-4.4.apk` 为输入，SHA-256：
`46b756fe289e47b3d41bec1a4d79da3c4fe752f3ece6a797ffca812d7216d1e8`。
不会重新编译整个 EasyPlay、替换 native 库或修改已经生成的普通 0.1.4 / 0.1.5 APK。

## 本地依赖

- Python 3、JDK 25、Android API17 `android.jar`，Android build-tools 的 aapt / zipalign / apksigner。
- jadx 1.5.6 内的 dexlib2 / smali / baksmali，JUnit 4 与 Hamcrest。
- 同目录归档中的 `easyplay-oem4/patch_brand.py`、`tools/DexTool.java`、`src/.../Bc03Schema.java`、`OemProtocol.java` 和对应测试。
- 私有核对输入：`.private/vendor-analysis/bc03-172/input.apk`、`bc03-172/gocsdk`、`bc03-179/input.apk`、`bc03/input.apk`（1.3.6）。系统输入及签名密钥均不在源码包内。
- 既有测试签名密钥 `.private/kitkat-probe-debug.jks`。采用其他密钥构建的包不能直接覆盖原包；不要为覆盖安装卸载并丢失记录。

工具路径在 `build.ps1` 中配置。完整构建：

```powershell
pwsh -NoProfile -File easyplay-bc03-172/build.ps1
python easyplay-bc03-172/package_public.py
```

脚本先核对所有必需输入指纹，比较 1.7.2 / 1.7.9 相关方法，运行协议/格式回归，再对六个类做增量修改。
仅两个逻辑位置变化：`Bc03Access` 构造器增加 1.7.2 版本；`OemTransport.<clinit>` 指纹数组增加 1.7.2。其余改动为版本显示、已核对版本列表与目标机型说明。
合并 DEX 后按类/方法进行审计，禁止额外逻辑和类集合变化；最终纯 V1 签名，API17 校验，versionCode 43，原包名、权限、minSdk、targetSdk 和三份 ARMv7 库保留。

原始 baseline 资源中的框架图标无法被不带 framework 资源的 aapt badging 解析，因此检查 Manifest 使用 `aapt dump xmltree`；本补丁不修改图标或资源表。

## 源码归档

`package_public.py` 仅归档列出的自写补丁、测试、构建工具、说明文档及既有自写依赖；不递归打包工作区。
不包含原始系统 APK / gocsdk、反编译输出、签名密钥、身份资产或用户日志。安装包单独分发。
