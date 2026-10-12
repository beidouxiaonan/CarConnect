# 构建说明

输入为已发布的 `CarConnect-0.1.5-beta-BC03-1.7.2-Android4.2-4.4.apk`，SHA256 `a66ca43a64a0537b955ee988b70a4b89a536584fe457f6e68ced183f703b2443`。放入工程 `artifacts/`；脚本拒绝其他基线。

依赖目录沿用之前项目：`.tooling/jdk25`、API17 `android.jar`、build-tools 的 D8/zipalign/apksigner/aapt、jadx 1.5.6、JUnit4 与 Hamcrest。Python 路径可用 `-PythonPath` 指定。编译桩只提供方法签名，单独做 D8 classpath，不打进 APK。构建不用原厂 APK 或 native 文件作为输入；运行时仍保留已核对的指纹门槛。

运行：

```powershell
./easyplay-bc03-172-spp/build.ps1 -PythonPath 'D:/Program Files/Python3.13.13/python.exe'
```

只覆盖自行编写的 OemTransport 及其内类、加入限时门槛与冷却类，另外更新四个显示版本的类。审计器要求其他类的逻辑完全保留，ZIP 审计只允许 classes3.dex、Manifest 版本、about.html 与 releases.json 改动。

签名沿用本机既有测试密钥 `.private/kitkat-probe-debug.jks`；源码包不提供密钥。其他人复现必须使用自己的密钥，无法以不同签名覆盖安装既有包。V1 启用、V2/V3/V4 禁用，minSdk17、targetSdk28 与现有权限/启动组件不变。

生成 `artifacts/CarConnect-0.1.6-beta-BC03-1.7.2-SPP-test.apk` 及 SHA256。构建后用 `package_public.py` 生成源码和说明附件；固定白名单排除输入 APK、OEM 二进制、私有缓存、日志及密钥。
