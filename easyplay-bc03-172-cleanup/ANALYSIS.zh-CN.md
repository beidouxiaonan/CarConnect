# BC03 1.7.2 SPP 恢复合并分析

## 输入与适配依据

精确业务基线仍为已发布的 0.1.6 BC03 1.7.2 SPP APK，SHA256 `58e31391441d603fd81bc3a20719a07de6c66ac139a7d708371a374ab61bd46c`。0.1.7 的清理功能在同一基线上重建并递增到 0.1.8 / code50，旧发布文件保留。

提供的 `D:/1.7.2/gocsdk` 与 OEM 已分析样本 SHA256 完全相同：`3e8f5687b69623087145177358c2104aeae50f2eb943db8711af983e395ced60`。因此同步已核对的命令路径，不按版本字符串猜测兼容性。运行时继续核对 BC03 APK 和 `/system/bin/gocsdk` 文件哈希；Bc03Schema 读取实际 BTFeature 接口，SppDisConnect 为 transaction48、无参 void，SPP getter 为50。1.7.2 APK 白名单为 `32b25ec4eacada6bad179e4015f1c9ae8f9ce4df4fee9ee8baf6e99a781ede58`，其他两版本沿用原白名单。

## 本次同步

原服务有两条 feature 路径：AtPound 使用缓存的 SPP index 请求 VH，AtPlus 同名断开入口为空。截图仅证明请求返回后状态未释放，不能仅凭截图确认车机正在走哪条路径。新版先保留原调用，等待无效后才回退。

该精确 gocsdk 的 VF/VG/VH 命令指针分别在 0xf22a8/0xf22ac/0xf22b0，对应 handler 表在 0xf1f1c/0xf1f20/0xf1f24；VH handler 位于 0xa0868（Thumb）。无参数分支循环调用 disconnect_spp，管理的索引为 0..8；带参数分支指定单个索引。采用完整无索引 ASCII 帧 `AT#VH` 加 CRLF，共7字节，不猜索引、不发送模块电源重启命令。构建通过 verify_native.py 核对哈希、分发表和关键分支字节，静态证据不等于实车成功。

SppRelease 同步 5 秒 Binder 状态等待、一次 native 回退和 12 秒状态等待、300ms 稳定释放、45 秒请求限流与总体状态等待上界。Binder 和 native 返回均不是成功；C3/C5 只在稳定释放后出现。取消、异常、状态抖动或重新占用继续失败。

OemTransport.startup 改为只读，不调用清理、不获取或释放 opening 锁。连接请求才执行清理，避免启动失败先消耗冷却。底层写入前再核对当前手机、记忆记录、模块地址、开关、自有通道和文件指纹。写入限定原 `/dev/goc_serial` 的 `/dev/pts/[0-9]+` 实路径，只写不读，不截取原厂 UART 响应。

## 保留 1.7.2 接入逻辑

不引入 OEM 版的另一套 VF 窗口，继续使用该分支现有 OemSppGate.RetryWindow 和 SppCleanupSequence。它们在所有清理检查/等待中验证 VF 窗口，旧流关闭只按 token 清除自己的请求。VF 写入前即登记 token，写入中断也保留保护。原 OemSppGate 的两个 DEX 类经 canonical 审计保持一致。

原 15 秒状态等待、仅1.7.2有限接入、8秒无接收关闭、原 iAP2 校验和 S1～S6 字节统计保持。native 释放也必须先通过状态检查，不能借 1.7.2 状态缺失探测绕过清理失败。新增衔接测试覆盖旧 VF 到期后的 native 释放、Binder 等待期间新 VF 阻止 native、native 写入无释放时不创建 VF。

## 限制

只有模块管理的 SPP 在本路径范围内。其他应用重连、服务状态缓存不更新、串口拒绝或未知固件仍会失败；不伪造 getter、不删除系统配对、不请求断开 HFP/A2DP、不修改 system。原厂 profile 联动需实车确认。状态等待上界不等同于保证厂商 Binder 或 PTY 调用本身绝不阻塞。
