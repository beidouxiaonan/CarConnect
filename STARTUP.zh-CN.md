# SD8227 安装成功但点击无反应：0.1.6 启动排查包

用户反馈旧 `CarConnect-0.1.4-SD8227-V1-test.apk` 可以安装，但点击图标没有反应。该包只改变签名/ZIP，运行代码与 0.1.4 完全一致。**目前没有车机启动异常堆栈，不能认定是蓝牙、USB、原生库或某个 API 导致。**

本包用于把启动失败变成可查看的结果，尚未在 SD8227 实机验证。不是已经确认根因的最终修复，也不处理 K2001N 的有线整机重启问题。

## 车机操作：三步

1. 从[测试发布页](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.6-sd8227-startup-test)下载 `CarConnect-0.1.6-SD8227-startup-test.apk`，直接**覆盖安装，不要卸载、清数据**。包名及签名证书相同，versionCode 为 44，保留已选择手机和原设置。
2. 从车机的应用列表打开 CarConnect，首次应出现“SD8227 启动检查”页面。先确认该页面能显示，再点击“启动 CarConnect”。如果旧桌面快捷方式仍无反应，从应用列表重新创建快捷方式；原 Activity 仍注册，正常启动时也有异常记录。
3. 如果原界面仍打不开，重新打开应用，点击“复制启动记录”，把记录私下反馈。未 Root 也能操作。若入口本身仍完全不显示，有电脑调试条件时用下方脚本；也请记录 APK 文件名、系统版本以及是否有系统报错。

旧业务主页面的版本文字仍为 0.1.4，表示沿用其功能代码；本次入口和 APK 系统版本显示 0.1.6。不要用旧主页面版本文字判断是否安装了新包。

## 为什么这样修改

- 启动 Application 改为 API17 编译的最小实现，先安装本机异常记录器，再调用原 `androidx.multidex.MultiDex.install`。MultiDex 安装抛异常时仍能显示检查入口；原安装可能耗时，底层卡死不能被 Java 异常处理器捕获。
- Launcher 改为独立轻量 Activity，所有入口和记录代码都在 **classes.dex**，不依赖 Kotlin、二级 DEX、CarPlay 控制器或原生库。检查入口关闭硬件加速，原 CarPlay TextureView 保持硬件加速。
- 首次需要手动点击进入；主界面成功进入 `onResume` 后，下次会自动进入。上次失败或启动中断时停在检查页，避免自动重试循环。
- 原主界面的 `onCreate` / `onStart` 内容原样移入私有方法，由薄包装保存阶段并捕获异常。进程其他 Java 未捕获异常保存堆栈后交给 Android 原来的崩溃处理器，不继续运行坏状态。
- `files/carconnect-startup.txt` 保留约 48 KiB 的最近记录，每条记录同步到磁盘；包括真实 SDK、Android 版本、ABI、机型、可用空间和异常堆栈。无网络上传、账号令牌或蓝牙地址采集。

## 记录如何判断

| 最后阶段 / 异常 | 下一步方向 |
| --- | --- |
| `MULTIDEX_BEGIN` / `FAILED MULTIDEX` | 看 DEX 解压、磁盘空间、加载器或 Dalvik 错误 |
| `ENTRY_VISIBLE` | 轻量入口已显示，尚未进入业务界面 |
| `MAIN_LAUNCH_REQUEST` 后失败 | 看 Activity 构造或类验证堆栈，例如 `VerifyError` / `NoClassDefFoundError` |
| `MAIN_CREATE_BEGIN` 后失败 | 看主界面构建、控件或设置读取的具体堆栈 |
| `MAIN_START_BEGIN` 后失败 | 看主界面启动及服务绑定；异步 Service 异常可能记录为 `UNCAUGHT` |
| `NoSuchMethodError` / `NoSuchFieldError` | 对照真实 SDK 与出错方法，随后针对缺失 API 适配 |
| `UnsatisfiedLinkError` / `dlopen` | 看缺失原生符号、依赖和 CPU/ABI |
| `MAIN_RESUMED` | Activity 已恢复到前台，不能据此断言 USB、无线或视频运行成功 |

原生崩溃、系统杀进程、断电、固件卡死或未进入 Application 的加载错误可能没有 Java 堆栈，需要系统日志。记录是诊断依据，不根据阶段名称猜测根因。

## 可选：电脑采集

需要车机 USB 调试和官方 platform-tools，无需 Root。先复现一次，再运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\easyplay-sd8227\collect-startup.ps1 -AdbPath C:\platform-tools\adb.exe
```

多台设备加 `-Serial 设备编号`。脚本读取系统属性、应用注册信息、应用私有启动记录（部分固件禁止 `run-as`）及 logcat；不卸载、不清数据、不清日志、不改系统。日志留在本地，检查个人信息后私下反馈。

## 构建

`build-startup.ps1` 需要已授权的旧 SD8227 V1 APK、既有签名密钥和本地 Android 工具。固定校验基础 SHA256 为 `6328988e7b4ae458b576b8cf233bb8d3c3b627d5c7fac476b243d02e46582055`。编译入口到主 DEX，再对原 Activity 增量包装、重新对齐和 V1 签名。源码包不含基础 APK、身份资产、签名密钥、OEM 二进制或用户日志。

仅改变 `classes.dex`、`classes3.dex`、`AndroidManifest.xml`。原有资源、assets、三个 ARMv7 原生库和 `classes2.dex` 逐字节保持一致；二级 DEX 中除原 Activity 包装外的类保持一致。原配对、USB/NCM、音视频和蓝牙实现不改。仍需在车机验证入口、主界面和连接行为。
