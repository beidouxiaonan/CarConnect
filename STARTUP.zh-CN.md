# 0.1.18 SD8227 全功能整合：启动与日志

1. 覆盖安装新 APK，保留应用数据。第一次在启动检查页点击“启动 CarConnect”；不要以安装成功替代主界面/无线实测。
2. 成功打开主界面后，确认原车蓝牙、热点和自动连接设置。已选 iPhone、音量和方向盘记录保留。
3. 无法进入时复制包含 `build=0.1.18-SD8227-unified-test` 的最新启动记录。主界面可用时，一键导出全部日志会同时包含启动/MultiDex/崩溃章节。

加载阶段依次记录 PROCESS_BEGIN、MULTIDEX_BEGIN、DEX_FACTORY_AVAILABLE/SELECTED、DEX_CREATE/APPEND、MULTIDEX_OK、MAIN_CREATE/START 和 MAIN_RESUMED。旧失败会保留在历史中，需按本次 build 与时间区分。

首帧等待仍从原车无线控制启动且前台有效 Surface 时计时；30 秒无画面才有限重连。蓝牙尚未接入或 SPP 保护暂停时，不能把它当作首帧恢复故障。

保留配对与设置，不要求安装原车替换 APK、ROOT 或每次安装后重启系统。长期原车占用目前仍无可靠普通 APK 清除保证。
