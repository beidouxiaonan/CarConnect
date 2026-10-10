# BC03 统一包构建

入口 `easyplay-bc03-unified/build.ps1`，随后运行 `package_public.py`。使用 API17 android.jar、Java、D8、dexlib2、zipalign、apksigner、JUnit。输出纯 V1 ARMv7 APK，versionCode54，同现有测试证书以支持保留数据覆盖安装。

两个输入基线必须精确匹配：OEM 0.1.10 SHA256 `c024cf21fb925fbf465ceeeb4031592559e0bbb5d4b738e1f0de1f8b702af8f7`；BC03 1.7.2 0.1.8 音频/全屏参考 SHA256 `3824782100cf0d14545c300ba8c4ad749966454e33fd6628d3093ff060e7f2d5`。三个 BC03 样本及 daemon 在私有目录提供，不满足输入校验时构建失败，不跳过验证。

```powershell
./easyplay-bc03-unified/build.ps1
& 'D:/Program Files/Python3.13.13/python.exe' ./easyplay-bc03-unified/package_public.py
```

`Bc03Profiles` 保存版本与文件匹配及缺失状态验证策略；`OemSocketOutput` 仅包装原始 LocalSocket 输出；`OemConnectDeadline` 管理单调时钟阶段期限。`OemTransport` 使用以上三个辅助类，并保留 SppRelease、SppRecoveryPrefs、OemSppPause、OemSppGate 的控制流程。已有 Binder schema 解析、VH 字节、首帧 helper 不修改。

AVOverlay 只导入此前固定的三项音频/Activity 补丁、七个辅助类和一项诊断方法，保持其规范化 DEX 与旧参考一致。Audit 限制允许替换的自有 transport/access 类、版本标签和明确的新辅助类，拒绝无关原 APK 类变化。原始 classes.dex/classes2.dex、so 库和其余资产逐字节核对。

公开包只选取自有 .java/.py/.ps1/.md、LICENSE 和声明的共享依赖。私有输入、生成的原 APK 反编译文件、签名密钥、身份资产、日志均不包含。详见 [验证](BC03-UNIFIED-VERIFICATION.zh-CN.md)。
