# CarConnect · K2001N / BC03 1.7.2 无线测试版

独立分支：`codex/bc03-172-k2001n`。目标车机：**K2001N_HDKJ_S112406 / QuadCore-T3**，原车外接蓝牙服务 **BC03 1.7.2**。

[下载本分支测试 APK 与补丁源码](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.5-beta-bc03-172-test)。

APK：`CarConnect-0.1.5-beta-BC03-1.7.2-Android4.2-4.4.apk`。
显示版本 `Carplay-connect-0.1.5-beta-BC03-172-test`，versionCode **43**，包名 `com.shihab.diplay.legacy`，最低 Android 4.2 / API 17，兼容 Android 4.4 / API 19。
这是 **普通 APK**，沿用此前 CarConnect 测试签名，可覆盖 0.1.4 / 0.1.5 并保留设置；不要求 Root，也不需要安装或替换用户提供的系统文件。

## 本次修改

- 新增用户提供的 **BC03 1.7.2** 服务版本及 APK SHA-256，继续检查实际 Binder 描述符、事务编号、手机信息格式与 gocsdk 文件指纹。
- 使用外接模块上报的手机地址和车机模块地址，沿用已核对的 `goc_serial` / `goc_spp` 数据通道与 iAP2 接续；不会把标准栈的 `t3` 当作原车模块。
- 保留已记住的 iPhone、自动连接、热点和音量设置，以及 0.1.5 的媒体预缓冲、音频线程调度和全屏恢复。
- 未更改 USB/NCM、视频、触摸、蓝牙字节流或系统权限。

**该 K2001N 此前点击有线连接会整机卡死并重启，原因尚未确定，本包没有修复这个问题。请使用无线模式测试，不要点击“有线连接”。** 有线问题应继续使用独立 USB 分阶段诊断 APK 定位。本次也没有实车验证，接口核对通过不等于无线 CarPlay 已打通。

## 最少操作步骤

1. **覆盖安装本包**，不要先卸载或清除数据。原来已记住手机的记录会保留。
2. 在原车蓝牙页面连接自己的 iPhone，确认 **通话连接已连接**；打开车机热点，iPhone 蓝牙和 Wi-Fi 保持开启，沿用之前无线成功时的热点配置。
3. 打开 CarConnect，确认是 **无线模式**。设置 → **无线连接、画面与音量设置** → 开启 **使用原车蓝牙连接 iPhone**。首次点击 **选择 / 更换 iPhone（记住）** → **记住并自动连接**。已有记录时无需重新选择；iPhone 弹出 CarPlay 允许提示时确认。

之后原车蓝牙连接到已记住的 iPhone、热点开启时，打开 CarConnect 等待即可。
CarConnect 保存的是“选择哪台手机”；原车配对和原车通话蓝牙自动回连仍由原车服务负责。如果原车不回连，需要在原车页面连接已配对手机，无需删除后重新配对。

## 连不上时看哪里

设置 → **连接诊断（故障时使用）**：先读取原车状态，再刷新并复制 **原车通道诊断**，同时保存原有连接日志。

| 诊断最后阶段 | 含义 / 下一步 |
| --- | --- |
| BC03 1.7.2，通话已连接，手机地址有效 | 状态读取成功，仍需数据通道与后续 iAP2 / Wi-Fi 接续成功。 |
| 文件指纹不匹配 | 实际安装的服务或 `/system/bin/gocsdk` 与提供文件不同；保存日志中的 SHA-256，重新核对实际文件。 |
| `goc_spp` 不可访问 / `goc_serial` 写入失败 | 普通 APK 无法访问端口或虚拟串口；需要具体权限错误，应用不能自行更改系统权限。 |
| VF 请求后没有 SPP 状态 | 请求已发送，但模块尚未建立数据连接；保存阶段日志，不是 APK 版本名称就能证明可用。 |
| 已交给现有 iAP2 握手、有收发字节但无画面 | 检查原有日志中的 iAP2、认证和 Wi-Fi 接续。 |
| 原车当前手机与已记住手机不同 | 在原车页面连接原手机，或明确选择并记住新手机。 |

连接失败后请正常断开再重试，不要连续点击。应用不会调用全局 SPP 断开，也不会抢占已有投屏通道。

## 文件核对与限制

本次提供的 `BC03BTService_AT+_HdLink_VP1.7.2.apk` 是 `com.bt.bc03` 1.7.2，最低 API 17。
提供的 `Bluetooth.apk` 则标注 `com.android.bluetooth` **7.1.1 / API 25**，与此前 Android 4.4 信息不同。仅凭这个文件不能认定车机实际系统版本；请以运行设备的 `Build.VERSION.SDK_INT` 为准。该系统 APK 不参与本补丁构建，也不要覆盖安装它。

详细核对见 [BC03-172-ANALYSIS.zh-CN.md](BC03-172-ANALYSIS.zh-CN.md)，构建见 [BC03-172-BUILD.zh-CN.md](BC03-172-BUILD.zh-CN.md)，验证见 [BC03-172-VERIFICATION.zh-CN.md](BC03-172-VERIFICATION.zh-CN.md)。
补丁源码包不含原始系统 APK、gocsdk 二进制、反编译源码、认证资产、签名密钥或用户日志。测试 APK 沿用原输入包中的实验性离线身份资产；本包不承诺官方认证或任意车机兼容。

[维护者主页](https://github.com/beidouxiaonan) · [项目](https://github.com/beidouxiaonan/CarConnect)
