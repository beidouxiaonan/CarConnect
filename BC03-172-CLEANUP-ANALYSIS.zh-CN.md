# 合并说明

精确输入为0.1.6 BC03 1.7.2 SPP测试APK，SHA256 `58e31391441d603fd81bc3a20719a07de6c66ac139a7d708371a374ab61bd46c`。不替换0.1.6原发布文件，独立递增到0.1.7 / code47。

运行时 SHA 白名单增加的1.7.2沿用原版值；Bc03Access支持1.7.2并新增无参void的断开入口。实际提供的1.3.6、1.7.9、1.7.2 APK由 Bc03Schema读取 `com.bt.BTFeature`，断开transaction48、SPP状态50、手机Parcel布局等均核对。未知签名/编号或未知文件不发送控制命令。原服务 HdLinkManager 转给 feature；AtPound 用当前SPP index请求VH，AtPlus的同名入口可能为空，因此返回值不能当作清理成功。

新增 SppCleanupPrefs、SppCleanupSettings、SppRelease，使用与OEM清理包相同的默认关闭/保存key、5秒释放等待、300ms稳定状态、45秒断开限流与自有通道保护。通过原有设置面板两处挂接启动检查和设置开关，不重编译原业务UI或引入更多权限。

两套45秒机制共存：SppCleanupSequence以包装的Monitor在所有清理前置检查/等待中先验证原有 RetryWindow。即使前一个字节流延迟关闭，也不会清除新VF请求的token；旧VF仍处于冷却时不查询/断开SPP，不因启动清理打断可能正在原车建立的请求。open现有cooldown检查继续保留在创建socket之前。

清理与open共用opening锁。startup按进程执行一次，在非UI线程绑定已运行服务；连接前再次检查。owner未关闭时不清理，超时/取消/地址变化均不进入VF。已发出的原车断开请求无法撤销；原厂profile联动需实车核对。

OemSppGate两类canonical smali原样保留；15秒状态窗口、仅1.7.2有限接入、8秒无接收关闭、实际iAP2校验、VF/地址/协议字节区分均保留。普通APK只能释放服务暴露的当前SPP，不能证明有其他应用占用、不能列举全部SPP owner，也不改写缓存状态强行成功。
