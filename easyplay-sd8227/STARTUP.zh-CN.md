# 0.1.7 旧系统 MultiDex 兼容测试：操作说明

## 本次已经定位的故障

0.1.6 完整启动记录显示实际 API 为 17，而显示版本为 Android 4.4.2。Application 在 `MultiDex.install` 中失败：系统没有程序查找的三参数 `makeDexElements(ArrayList, File, ArrayList)`。原 APK 的加载调用固定走 V19 路径，缺少旧系统路径；错误发生在主界面创建之前。

设备上报 `AC822X / ac8317 / autochipsac83xx`。型号俗称不足以确认芯片、API 或固件实现，不能据此判定是 MTK8227。两参数方法是否存在仍待新包的运行时探测确认。本版针对已确认的加载方法不兼容，尚无新包实车结果。

## 车机只需三步

1. 在 [0.1.7 测试发布页](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.7-sd8227-multidex-test) 下载 `CarConnect-0.1.7-SD8227-multidex-test.apk`，覆盖安装，**不要卸载或清数据**。同包名、同签名，versionCode 48 高于旧包 44，保留原设置和已选择手机。
2. 从应用列表打开 CarConnect。此前状态是 failed 时会停在“启动检查”页；点击“启动 CarConnect”，先确认原主页面能打开。首次 DEX 优化可能需要等待，避免重复点击。
3. 若仍打不开，回到启动检查页点击“复制启动记录”，私下反馈**包含 `build=0.1.7-SD8227-multidex-test` 的最新记录**。旧错误会保留在日志前面，不能据此判断新包失败。旧桌面入口无反应时，从应用列表重新创建快捷方式。

APK 与检查入口显示 0.1.7；原业务主页面仍显示 0.1.4，表示沿用其业务实现。后续主界面成功恢复至前台后，下次自动进入；发生异常或启动中断时停在检查页，避免自动重试循环。

## 新记录如何看

| 记录 | 表示什么 |
| --- | --- |
| `DEX_FACTORY_AVAILABLE` | 系统实际提供的工厂方法签名，仅记录方法，不记录手机地址 |
| `DEX_FACTORY_SELECTED ... (ArrayList,File)` | 选中了两参数旧系统路径 |
| `DEX_FACTORY_SELECTED ... (ArrayList,File,ArrayList)` | 选中了三参数路径；也支持限定的 List 变体 |
| `DEX_CREATE_BEGIN` / `DEX_CREATE_OK` | 正在加载二级 DEX / 所有返回元素校验成功 |
| `DEX_APPEND_OK` → `MULTIDEX_OK` | 二级 DEX 已加入原加载器，MultiDex 调用完成 |
| `MAIN_CREATE_BEGIN` / `MAIN_START_BEGIN` | 正在创建原主界面 / 启动与绑定服务 |
| `MAIN_RESUMED` | 主界面已恢复前台，不能等同于有线、无线或音视频实测成功 |
| `FAILED MULTIDEX` | 看最新工厂签名及堆栈；未知签名不会强行调用 |
| `FAILED MAIN_ACTIVITY` / `UNCAUGHT` | 加载阶段可能已通过，需要继续根据新的异常适配 |

Java 异常、方法缺失、DEX 文件读取失败可记录；原生崩溃、系统杀进程或固件卡死未必留下完整 Java 堆栈，可能还需要 logcat。检查入口没有 USB 或蓝牙连接行为。

## 修复范围

- 保留原 `androidx.multidex.MultiDex.install`、Extractor 的缓存、文件锁、CRC 和 IOException 时单次重新提取。只把 `installSecondaryDexes` 中一个 V19 调用替换为自编 `LegacyDexInstaller.install`。
- 根据实际方法签名选择工厂，不根据界面 Android 版本决定。限定支持 `makeDexElements` 的两/三参数 List、ArrayList 形式及第四参数 ClassLoader 形式；不支持时保存可用签名并停止。
- 方法只选一次，调用出现错误时不会切换到另一方法。校验返回元素数量、类型与实际 `dexFile` 非空，特别防止旧工厂隐藏 I/O 错误后返回仅含 ZIP 的元素。所有检查成功才追加，保留主 DEX 的查找优先级，失败不会把部分二级 DEX 加入类路径。
- 入口、适配器和记录类全部在主 DEX，API17 编译。原 Activity 的业务方法保留，启动异常包装与 0.1.6 相同。检查入口软件渲染；原 CarPlay TextureView 保持硬件加速。
- 记录存于本机 `files/carconnect-startup.txt`，保留约 48 KiB，每条同步保存；增加构建标识、VM 版本、方法签名和追加阶段。没有网络上传。

## 可选电脑采集

先复现一次，开启车机 USB 调试后执行；无需 Root，部分固件可能禁止 run-as：

```powershell
powershell -ExecutionPolicy Bypass -File .\easyplay-sd8227\collect-startup.ps1 -AdbPath C:\platform-tools\adb.exe
```

多台设备加 `-Serial 设备编号`。脚本读取属性、注册信息、本机启动记录和 logcat，不卸载、清数据或清日志。日志检查个人信息后私下反馈，不上传公开仓库。

## 构建和来源

执行 `build-startup.ps1`，需要已授权 `CarConnect-0.1.4-SD8227-V1-test.apk` 与既有签名密钥。固定基础 SHA256：`6328988e7b4ae458b576b8cf233bb8d3c3b627d5c7fac476b243d02e46582055`。以与 0.1.6 相同基础重建启动包装，再加入兼容加载器。

只改变 `AndroidManifest.xml`、`classes.dex`、`classes3.dex`。原资源、assets、三个 ARMv7 库、classes2/4.dex 逐字节不变；不修改原配对存储键、USB/NCM、蓝牙或音视频协议。源码包只含自编补丁及文档，不含基础 APK、身份资产、签名密钥、OEM 二进制或用户完整日志。具体主机验证见 [VERIFICATION.zh-CN.md](VERIFICATION.zh-CN.md)。
