# 12306XposedPlugin
辅助12306抢票的xposed插件。支持秒杀、捡漏、自动下单等操作，仅限于技术交流请勿滥用。

## 安装插件
![](./screenshot/code.png)

**⚠️ 请主动授予悬浮窗权限!**

## 配置
**⚠️ 请先启动12306客户端并登录完成，点击图中的+添加任务，添加完成后记得长按选中任务✅**

![](./screenshot/s1.png)

## 注意事项
- 需要授予应用悬浮窗权限!!!
- 国内ROM禁止了应用相互唤醒，可能得同时启动12306客户端和本应用
- 日志调试TAG, 协议相关用 `rpcCallRequest|rpcCallResponse`, 12306日志用 `H5Log|Hook12306`, 插件启用成功后会自动开启`chrome inspect`

## 待解决问题❓
- VirtualXposed 内运行12306客户端提交订单提示**702错误(SIGN\_VERIFY\_FAILED)**，所以暂时不支持免ROOT运行该插件
- 掉线后自动登录功能正在开发中，目前请主动登录
- 暂不支持改签抢票 (欢迎贡献代码)

## 技术交流
![](./screenshot/qq.png)

## 其它信息
- [车站信息](https://kyfw.12306.cn/otn/resources/js/framework/station_name.js?station_version=1.9027)
