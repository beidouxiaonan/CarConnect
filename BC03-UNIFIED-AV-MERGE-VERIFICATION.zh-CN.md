# 0.1.5 音频与系统栏优化合并核对（2026-10-10）

已发布的 **0.1.11 BC03 统一测试包完整包含 0.1.5 AV 测试版的音频和系统栏优化**。之前通过 1.7.2 的固定 AV 参考包带入，这次直接用用户指定的 0.1.5 发布 APK 与源码包核对，确认全部运行逻辑及实际调用入口一致。

[0.1.5 指定版本](https://github.com/beidouxiaonan/CarConnect/tree/v0.1.5-beta-av-test) · [使用现有 0.1.11 APK](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.11-beta-bc03-unified-test)

## 合入内容

| 部分 | 0.1.11 中的对应行为 |
|---|---|
| 媒体起播缓冲 | 按已保存的300/500/1000ms设置及实际PCM容量预缓冲；未知容量退回即时播放，避免停止状态写满缓冲 |
| 断流恢复 | 缓冲耗尽且近期无数据时暂停补缓冲，保留排队音频；短音频尾部仍播放 |
| 音频线程 | 解码/写入线程请求nice -8，UDP接收线程请求nice -4；拒绝时保留原调度 |
| 导航/Siri/通话 | 非MEDIA通道即时播放，保留现有独立音量逻辑；若手机混入MEDIA则跟随媒体缓冲 |
| 系统栏恢复 | 手动启动、恢复前台、窗口重新获得焦点、进入CarPlay时请求本窗口全屏；有限延迟重试，离开时取消 |
| Android适配 | API19及以上请求0x1006，API17/18请求0x6；保留已有E03导航栏例外 |
| 诊断 | 音频缓冲/写间隔及窗口请求值记录，刷新与复制入口 |

这些是本应用窗口的标准全屏请求。厂商独立悬浮快捷栏仍可能需要该车机Launcher/SystemUI资料，不能声称已隐藏所有厂商Dock。媒体缓冲不能补回已丢失的音频，也不能保证高负载驱动稳定。

## 精确验证

- 0.1.5 AV APK SHA256：`46b756fe289e47b3d41bec1a4d79da3c4fe752f3ece6a797ffca812d7216d1e8`，与指定Git标签中的校验值匹配。
- 0.1.11 APK SHA256：`c8fcfc38635b3a2cce6600004f8df59f50c2f922c7cf1283caa462f65e318b61`，与已发布校验值匹配。
- 5个自有AV源码与指定Git标签中的源码ZIP逐文件对比（仅统一换行），内容一致。
- 对比完整classes3.dex中10个相关类的规范化DEX，全部一致；包含LegacyAudioRenderer/AudioStream/LegacyActivity的实际挂载，未仅检查辅助类。
- EnhancementPanel.showDiagnostics整方法一致，仅允许一处BC03兼容说明从1.7.9/1.3.6改为1.7.2/1.7.9/1.3.6，未忽略方法指令。
- 既有117项主机回归已经包含17项音频/全屏测试。本次新增直接参考审计，构建入口强制执行，不新增运行逻辑。

下列摘要仅为自有补丁与调用集成的规范化散列，不包含原APK反编译代码或用户日志：

```text
MATCH Lcom/shilapi/xcertplay/media/LegacyAudioRenderer; SHA256=ab163dc43bcc2b1a135d188e7eb9afad4c1cea9e5351a189d6c51bc398069c28
MATCH Lcom/shilapi/xcertplay/airplay/AudioStream; SHA256=4ce9c903e772856a31d3768fcfa07669b7c9b71efbc3eee2432b61f1feeffa0b
MATCH Lcom/shilapi/xcertplay/legacy/LegacyActivity; SHA256=3b3556bdada8cce20d17c68f89e3dcd6b43693fbf2a9881e2c1cc7e449a53b99
MATCH Lcom/shilapi/xcertplay/patch/PcmBufferState; SHA256=763af604e15384edcc372577ad21b61a1f5c79edac29b964a3fdce71c26b4260
MATCH Lcom/shilapi/xcertplay/patch/StableAudioTrack; SHA256=d8db4bc8cc71180710c2cc828715da1a1201e4432a57efecbb02aca23663fd84
MATCH Lcom/shilapi/xcertplay/patch/AudioRuntime; SHA256=c09c378d9d7a4a6b5509cff478733e4b520629ddabe5983894567a98ad9995bf
MATCH Lcom/shilapi/xcertplay/patch/FullscreenRecovery; SHA256=22e72ebaf26906da3dae48ac8f8dd211913ea98201063e586bd9953509878bbf
MATCH Lcom/shilapi/xcertplay/patch/FullscreenRecovery$Controller; SHA256=a334d279ba86ed1f823d721fb74fd96e6f02e57f2acf34444bf44b7e63a6fa97
MATCH Lcom/shilapi/xcertplay/patch/AvDiagnostics; SHA256=7a39f7096ed9498ea430dbc53a5a396986eb10da387c8b550da9255965caf959
MATCH Lcom/shilapi/xcertplay/patch/AvDiagnostics$1; SHA256=268870bb81ec385487bc707981d520dd6d836300ae44447ebb28566913117e3c
MATCH EnhancementPanel.showDiagnostics SHA256=77045c3e1a073c17147a2254f2aa5cb28d92c7ec31a6b777f91563b1eb205db5
Complete released AV classes matched=10; diagnostics method matched=1 (one firmware wording substitution)
```

## 使用与后续维护

继续使用0.1.11统一测试APK，同证书覆盖安装保留手机与设置。原已发布APK、源码ZIP、校验值和Git标签保持原始内容；新增校验代码与本记录在当前codex/bc03-unified分支，发布页追加此记录。无需改回0.1.5才能使用这些优化。

当前构建新增 `verify_av_reference.py` 和 `tools/AvReferenceAudit.java`，必须提供精确0.1.5 AV参考APK；参考不匹配或任何相关类/调用入口有差异即失败。它们用于以后构建防止遗漏，无需Root或更换系统蓝牙。

BC03统一适配、清理保护、socket阶段期限、已保存手机和首帧恢复保持现有0.1.11行为。实际车机音频与厂商快捷栏效果仍需复测；K2001N有线重启、SD8227专用启动分支不在本次修正范围。
