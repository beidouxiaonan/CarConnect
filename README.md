[完整方案：可行性、适配范围与文件清单](docs/OEM-WIRELESS-FEASIBILITY.zh-CN.md) · [下载离线图文说明](https://github.com/beidouxiaonan/CarConnect/releases/download/v0.1.9-beta-oem-first-frame-diagnostics-test/OEM-WIRELESS-FEASIBILITY.zh-CN.html)

[下载测试 APK 和补丁源码](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.9-beta-oem-first-frame-diagnostics-test)

# CarConnect 0.1.9 OEM 首帧恢复与 SPP 冷却等待测试

面向用户确认的 **BC03 1.7.9**，沿用 `codex/oem014-spp-cleanup` 的 OEM 0.1.8 精确基线。本包不是 BC03 1.7.2 接入版，也不包含 SD8227 启动补丁。

[下载测试 APK](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.9-beta-oem-first-frame-diagnostics-test)。本机检查已通过，仍需车机实测。普通 APK 无法承诺释放所有系统 SPP；本版不把命令写入当作清理成功。

## 使用方法

1. 覆盖安装 `CarConnect-0.1.9-beta-OEM-first-frame-diagnostics-test-Android4.2-4.4.apk`。**保留应用数据，不卸载**。同包名、同证书，code51，保留已选 iPhone、自动连接、热点、音量和清理开关。
2. 原车蓝牙先连接已配对的 iPhone，车机热点保持开启。勾选“自动连接”，并沿用已开启的原车蓝牙、SPP 清理选项。新“无线无首帧时自动重连”默认开启，可关闭并保存。
3. 打开 CarConnect 后等待这一轮完成。出现 **C0** 时正在等待冷却，最多再等 45 秒；期间没有重复发送 VH/VF，不需要反复点击或换手机。真正仍占用时继续执行原来的 Binder、VH 清理，失败仍明确停止。

已选择的 iPhone 使用原 `legacy-settings` 的 `phone` / `phoneName`，在连接前通过 `commit()` 保存。正常重启后无需再选；换手机、清除数据或卸载重装时才需重新设置。保存手机并不代替原车蓝牙的自动回连；原车通话蓝牙尚未连接时，CarConnect 仍会等待这台已保存的 iPhone。

## 无首帧恢复

仅用于已开启原车蓝牙路径的无线连接。从无线控制启动开始，应用在前台且视频 Surface 有效时计时。**连续 60 秒没有当前会话首帧**才关闭本次 CarConnect 会话，通过原来的关闭线程和 15～60 秒退避重新尝试。最多自动恢复两次；仍无首帧时保留当前监听并显示检查授权、热点的提示，不无限断连。手机首次授权时请及时确认。

首帧真正显示后取消恢复。退出到后台、Surface 丢失会重新开始前台等待；手动关闭自动连接、关闭恢复开关、切到有线、旧 generation、controller 被替换或服务销毁时，不再执行该恢复。关闭再开启“自动连接”可开始新一轮；服务重建也开始新预算。

## SPP 诊断

新增 `SPP 观察`，记录 Binder 缓存、缓存变化次数、最近 SPP 广播及清理请求后的广播次数。广播只作诊断，不授权接入。

- C0：同一任务等待原来的冷却结束，或在等待中观察到通道稳定释放。
- C2 / C4：原接口返回或 VH 写出；**不等于释放**。
- C3 / C5：未连接标志稳定 300ms，允许继续原连接。
- `未观察到新回执`：不能区分缓存残留和真实占用。
- `断开广播与缓存不一致`：提供了进一步排查线索，仍停止接入。
- R1 / R2 / R3：首帧计时 / 首帧已显示 / 超时恢复。

若仍卡在 C4 后超时，复制最新的“原车通道诊断”和“诊断信息”完整文本，私下反馈；不要只截冷却倒计时。原车关闭再开启蓝牙可作为一次人工排查，会中断原车通话和蓝牙音乐，请在没有通话时操作。本包不自动重启蓝牙，不调用原厂 `btReset` 或删除配对。

详见 [分析](OEM-RECOVERY-ANALYSIS.zh-CN.md)、[构建](OEM-RECOVERY-BUILD.zh-CN.md)、[验证](OEM-RECOVERY-VERIFICATION.zh-CN.md)。公开补丁源码不含原 APK 的身份资产、密钥、OEM 二进制/反编译代码或用户日志；测试 APK 沿用用户已授权的原输入包实验性离线身份资产。
