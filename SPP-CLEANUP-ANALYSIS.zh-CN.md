# 接口核对与实现边界

0.1.4 OEM 包在发现 `isSppConnect()` 为真时立即拒绝打开原车通道，以避免接管其他应用连接。本次按用户要求增加保存的可关闭清理开关。

对用户提供的 1.3.6 / 1.7.9 文件核对：`com.bt.BTFeature.SppDisConnect()` 为无参 void，Binder transaction48；Proxy 写 interface token 后直接 transact/readException，没有 phone 参数和 bool 返回值。BTManager 转交 HdLinkManager，再交给原车 feature。AtPound feature 会通过原车 UART 发送带当前 SPP index 的 VH 请求；AtPlus 的相应入口可能为空，因此请求返回不能当作已清理。

运行时先核对 BC03 APK 和 gocsdk SHA256，再读实际 AIDL 描述符、方法签名、transaction 常量及已核对手机 Parcel 布局。普通 APK 通过原服务断开，不直接猜 index、写 VH 或读取 UART，也不枚举未知 SPP 实例。

开关默认关闭、保存在原设置 SharedPreferences 的独立 oemSppCleanup key。只有原车蓝牙选项和清理选项同时开启才会在启动后台安排检查。启动检查和连接 open 共用 opening 锁；own current 未关闭时禁止清理。启动检查按进程执行一次；原车服务未就绪时连接前再检查。

连接前在开关快照、运行代数/取消检查下调用 SppRelease。状态为真时只发送一次断开，观察最多 5 秒，连续 300ms 为假后通过；45 秒限流即使调用异常也保留。调用完成后重新核对手机、模块 MAC 和记住的手机，再读 SPP，变化或重新占用时不发送 VF。关闭开关或取消后停止后续操作；已经发出的原车请求无法撤销。

SPP 状态为真不能证明有其他投屏软件在运行，可能是原服务缓存状态。原接口不提供所有 SPP owner 列表，所以本版不声称“杀掉所有后台 SPP”。HFP/A2DP 与 SPP 是不同 profile，不以重置蓝牙清理通道。原厂 profile 联动、后台应用立即重连、状态异常或模块不支持仍须实车排查。
