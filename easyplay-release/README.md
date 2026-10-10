# CarConnect 0.1.12 · BC03统一适配 · 音画与方向盘优化

[正式版下载](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.12) · [完整功能与优化](FEATURES.zh-CN.md) · [操作手册](USAGE.zh-CN.md) · [适配范围](COMPATIBILITY.zh-CN.md) · [验证](VERIFICATION.zh-CN.md) · [构建](BUILD.zh-CN.md)

同一 APK 使用已经核对的 BC03 1.3.6、1.7.2、1.7.9 配置，包含 0.1.5 的音频缓冲和标准全屏优化、手机记忆、有限首帧恢复、SPP 保护，以及本次方向盘事件时间修正和旧安卓前台媒体入口维护。

- 安装包：`CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7.apk`。
- 启动器名称：**CarConnect BC03**；设置标题：**CarConnect 0.1.12 · BC03/AV/Wheel**。
- Android 4.2+/API17，含 4.4/4.4.2 分流；**ARMv7 32 位**，纯 V1 签名。
- versionCode55；保留包名 `com.shihab.diplay.legacy` 和已有证书，覆盖安装保留手机、音量及映射记录。

`BC03-Unified` 表示核对过的三个配置，不是 1.7.2～1.7.9 区间的全部版本保证。`AV` 表示音频/画面及系统栏优化，`Wheel` 表示方向盘修正。GitHub 发布标记为正式版本；本次新增改动通过主机测试，目标车机效果仍需实测。K2001N 有线整机重启、SD8227 专用启动以及独立厂商 Dock 的限制继续保留。

公开目录只包含自有补丁代码和说明。完整应用仍依赖授权提供的原 APK；源码包不含原厂 APK/gocsdk、生成反编译代码、身份资产、签名密钥或用户日志。APK 沿用授权输入已有的实验性离线身份资产，沿用原许可证及第三方声明。
