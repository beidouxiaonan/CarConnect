# BC03 1.7.2 实际文件核对

日期：2026-10-07。目标 K2001N_HDKJ_S112406 / QuadCore-T3。仅静态检查用户提供文件，没有车机、Root 或 adb 实测。

## 输入指纹

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| BC03BTService_AT+_HdLink_VP1.7.2.apk | 557492 | `32b25ec4eacada6bad179e4015f1c9ae8f9ce4df4fee9ee8baf6e99a781ede58` |
| Bluetooth.apk | 792325 | `995942d2a20c53b300ea782031b65a6db25a996a92510dd099b1254a6150dadc` |
| gocsdk | 993828 | `3e8f5687b69623087145177358c2104aeae50f2eb943db8711af983e395ced60` |

gocsdk 与此前 1.7.9 / 1.3.6 方案核对的二进制完全相同。因此沿用此前静态验证过的虚拟串口和 Unix socket 协议，不重新推测 AT 指令或标准 RFCOMM 接口。

## Binder / Parcel

包 `com.bt.bc03`，versionCode 172，versionName 1.7.2，minSdk 17。服务 `com.bt.bc03.BTService`，Binder 描述符 `com.bt.BTFeature`。
Manifest 中服务带 intent-filter，没有指定绑定权限、没有显式禁止导出；运行时是否能绑定仍需实测。

| 只读方法 | 返回类型 | 事务编号 |
| --- | --- | ---: |
| isBlueToothPowerOn | boolean | 3 |
| isConnectDevice | boolean | 6 |
| isConnectHFP | boolean | 35 |
| isConnectA2DP | boolean | 36 |
| getLocalDeviceName | String | 10 |
| getLocalDeviceAddress | String | 20 |
| getConnectDevice | BTDevice | 38 |
| isSppConnect | boolean | 50 |

实际 DEX 由现有 `Bc03Schema` 解析，不加载或执行 OEM 类。
`BTDevice` 的 Parcel 前两项为地址字符串、名称字符串，后面为设备类别、服务、是否配对、配对索引及 OBD 密码。应用只读取前两项并释放 Parcel；不会把名称当作 MAC。
`getConnectDevice` 读取通话连接设备，`getLocalDeviceAddress` 读取外接模块本地地址。二者、HFP 与 SPP getter 的实现和已核对 1.7.9 逐方法比较。

## 为什么继续使用 gocsdk 方案

1.7.2 的 `BTManager.SppConnect` 转交 `HdLinkManager.SppConnect`，后者仅输出日志后返回 true，没有实际调用蓝牙连接。
因此把 Binder 返回 true 当成“已连接”会产生假成功。本补丁不使用这个入口。

应用沿用以下既有流程：核对已安装 BC03 与 gocsdk 指纹 → 读取通话连接手机和模块 MAC → 检查没有已有 SPP → 验证 `/dev/goc_serial` 指向 `/dev/pts/<数字>` → 接入 `/dev/socket/goc_spp` → 通过既有虚拟串口发送单次带 iAP2 UUID 的 VF 请求 → 等待 SPP 状态 → 发送既有 socket 地址握手 → 交给原 iAP2 流程。

不调用标准栈 BluetoothSocket、不重启或替换系统服务、不更改文件权限。普通 APK 是否能访问上述设备节点、模块是否支持 UUID 接续、实际运行文件是否相同，均需本车机测试。

## Android 版本矛盾

用户提供的 `Bluetooth.apk` Manifest 为 `com.android.bluetooth`，sharedUserId `android.uid.bluetooth`，版本 7.1.1 / versionCode 25，minSdk / targetSdk 均为 25。它不是 API19 标注的蓝牙包，可能属于另一套固件或车机显示版本与运行 API 不同；静态文件无法判定是哪种情况。
本补丁不依赖或分发这个系统 APK，使用外接 BC03 服务。没有据此把车机认定为 Android 7.1，也没有据此宣称 4.4 不兼容。

## 有线重启是独立问题

该车机此前在 CarConnect 点击有线连接后卡死并重启，接线本身正常。现有证据不足以定位 USB 配对、NCM 配置、驱动或其他阶段。本补丁只新增无线服务版本和指纹；没有修改 USB/NCM，也不声称解决整机重启。
