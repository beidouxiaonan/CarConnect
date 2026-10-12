# 0.1.9 OEM 分析（2026-10-09）

## 发布后的实车反馈

OEM 0.1.9 / BC03 1.7.9 最新截图仍停在清理阶段。Binder 缓存为连接，状态变化与清理后的广播均为零；请求 SppDisConnect 及写出一次 VH 后，最终清理超时并停止，未发送 VF。截图另有一次“原车蓝牙连接已取消”，需要完整控制器/服务日志确定是中断、旧 generation 还是会话关闭，不能据此断言有固定 15 秒 OEM 超时。

原控制器的标准 BluetoothSocket 连接超时与 OEM 的 OemTransport.open 分支不同；后者不经过标准 socket 超时入口。首帧恢复仅在无线控制启动后计时，不会因为正在清理 SPP 而启动。当前失败不是首帧计时未重连，也没有足够证据识别占用应用或确认缓存失效。后续应先取得单轮完整会话与原车服务回执，不能改写状态或绕过占用门禁。

用户确认固件为 BC03 1.7.9。新截图中的版本也是 `0.1.8-beta-OEM-SPP-native-recovery-test`，不将浏览器打开的 1.7.2 发布页当作安装版本。截图显示 C4 后仍超时且“未发送 VF”；这属于接入前的清理问题，与另一项“握手后等待 iPhone、没有首帧”反馈分别处理。

## 可确定与不能确定的事实

已提供的 1.7.9 APK 中，BTManager 的 `isSppConnect()` 转交 HdLinkManager 的内存标志；该标志依赖 `onSppConnnected` 回执更新。AtPound 的 SPC/SPS 事件负责连接/断开，AT+ 的 SPP 断开入口为空。不同协议路径不能一概而论。

现有 VH 静态证据来自已核对 gocsdk 的精确 SHA256，确认了无索引断开模块管理的 SPP 0..8 分支；它不证明车机执行成功或 BC03 已接收到回执。截图不能区分真实占用、重新接入和旧标志残留。新增广播观察仅用于进一步诊断，不能把广播或“写出命令”当成成功依据。

底层 SY 查询输出 status/index 格式；不假定 BC03 的 SPC/SPS 解析会更新它。goc_control 是原车服务使用的控制端口，也不为查询抢读共享串口。原厂 `btReset` 的 AT+ 路径使用 DFLT，不用它实现普通 SPP 恢复。

## 冷却等待

原版本在 45 秒清理冷却内抛出 IOException，可能造成“失败→自动重试→仍在冷却”的循环。新版在已经持有的单个连接任务内等待剩余时间，使用既有 worker 与 opening 锁，150ms 检查取消和通道状态。期间不再发断开命令，空闲状态稳定 300ms 可提前继续；冷却结束后重新核对状态并允许一次原清理。

手机选择改变、关闭开关、controller 取消会退出等待。真正调用 Binder 断开前还会重新读取当前手机和模块地址，避免长时间等待后断开已更换手机的连接。清理等待仍限 5 秒 Binder 阶段 + 12 秒 native 阶段 + 稳定窗口；冷却等待最多额外 45 秒，不是清理总耗时的固定值。旧 VF 45 秒保护及本应用通道所有权保护保留。

## 首帧恢复

对 LegacySessionService 的当前 generation 使用类型化 RunningWireless/WirelessActive 状态挂接，不依赖中文日志字符串。每秒在原 main Handler 检查当前 controller、自动连接、OEM 选项、有线模式、销毁/关闭状态、前台 Activity、有效 Surface 及原本的 `videoFrameSeen`。

60 秒等待给正常握手和手机授权留出时间；重复 Ready 状态不重置期限。超时通过 `stopSession(true,true)` 调用原 worker 关闭和自动退避，不直接新建并行 controller。每个 Service 等待周期最多恢复两次，自动关闭不重置预算；手动停止或真实首帧重置。关闭前在 UI 回调之后再次验证归属，防止同步回调换会话。

首帧计时只在无线控制启动后生效，不能修复接入前的持续 SPP 占用。SPP 诊断与冷却等待正是为了处理、区分后一类问题。

## 手机保存

原有 RememberedPhone、OemWireless、LegacyPhoneSelector 与所有存储键保持不变，DEX 审计确认它们未被替换。正常重启不用再选，但不会强行打开用户关闭的自动连接，也不替代原车 HFP 回连。
