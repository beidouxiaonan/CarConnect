# 0.1.9 OEM 构建

输入为已经发布的 OEM 0.1.8 APK，SHA256：

`75133d5340d7cc7c7fc6ad50ae4ad0b1b9e40b031390243d5937cbd1742cb856`

将合法取得的输入放到 `artifacts/CarConnect-0.1.8-beta-OEM-SPP-native-recovery-test-Android4.2-4.4.apk`。构建脚本读取它，并严格拒绝不同输入；不使用 shared 目录的其他未提交修改。生成的反汇编文件在 `.private`，不发布。

工具位置沿用已有项目：`.tooling/jdk25`、API17 `android.jar`、build-tools/android-15、jadx 1.5.6 包中的 smali/dexlib2、JUnit4.13.2 与 Hamcrest。Python 默认 `D:/Program Files/Python3.13.13/python.exe`，可通过 `-PythonPath` 更改。

```powershell
./easyplay-first-frame/build.ps1
& 'D:/Program Files/Python3.13.13/python.exe' ./easyplay-first-frame/package_public.py
```

以 API17 编译新增 Java helper、运行回归测试、D8 min-api17、精确挂接五处 UI/Service 调用，严格审计非目标类，再替换 classes3.dex、版本清单和两项说明资产。minSdk17 / targetSdk28 / ARMv7 保留。签名使用本地 `.private/kitkat-probe-debug.jks` 的原测试证书，纯 V1；密钥不分发，其他构建者需替换自己的签名步骤，不能使用不同证书覆盖安装已有测试版。

`stubs` 是仅编译依赖，`test-stubs` 是 JVM 假对象。两者不合并入 APK；已存在的 Binder schema、VH/VF 帧、手机保存和业务类由精确基线提供。源码包包含执行脚本需要的自编工具、compile-only 依赖、License 与 Credits，不含 SDK/JDK、输入 APK、OEM 二进制或原包身份资产。
