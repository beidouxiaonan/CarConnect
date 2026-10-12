# 0.1.12 构建说明

当前入口 `easyplay-release/build.ps1`。输入必须为已授权的 0.1.11 APK，SHA256 `c8fcfc38635b3a2cce6600004f8df59f50c2f922c7cf1283caa462f65e318b61`；不匹配立即停止。

```powershell
./easyplay-release/build.ps1
& 'D:/Program Files/Python3.13.13/python.exe' ./easyplay-release/package_public.py
```

使用 Python、JDK、API17 android.jar、JUnit、D8/dexlib2、zipalign/apksigner 和本地既有签名密钥。工具路径示例沿用本工作区；在其他机器需要调整工具和输入路径。密钥和基础 APK 不公开。

新增三个自有 helper：`WheelEventClock`、`WheelInputRecovery`、`MediaKeyRecovery`。映射方法保存为 Original，薄入口先处理事件时间再调用原逻辑；清理随原关闭方法执行。Activity 仅在原全屏通知旁增加五个前台状态通知，原 AV helper 和调用继续保留。

构建包含 25 项新增主机回归、API17 编译、原类规范化审计和 ZIP/DEX/ELF/签名校验。主机 Android fakes 只在独立测试 classpath，不能进生产 DEX。既有统一版源码、测试和构建位于 `easyplay-bc03-unified`，基线 117 项回归也保留。

生成文件、原厂反编译代码都留在 `.private/apk-analysis/easyplay/release-012`。公开源码 ZIP 仅收集自有源码、脚本、说明及许可证，不包括授权 APK、原厂系统二进制、离线身份资产、密钥、个人日志或构建缓存。
