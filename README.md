# 平安回家 · 多门禁版

基于 [dogproton/SafeBaiyun](https://github.com/dogproton/SafeBaiyun) 的白云区蓝牙门禁离线客户端。
沿用原有门禁协议，需要每个门禁已有的 MAC 地址和 Key。

## 使用

1. 点击右上角 + 或「添加门禁」，填写名称、MAC 和 Key。
2. 可添加多个门禁，每张卡片可开门、编辑、删除或设为默认。
3. 首次开门按提示授予附近设备权限，并开启蓝牙。
4. 桌面快捷方式和小部件解锁默认门禁。在 App 内切换默认门禁即可改变它们的目标。
5. 已安装版本能够保留数据升级时，原有单门禁会自动迁移为「原有门禁」。

MAC 格式为 AA:BB:CC:DD:EE:FF，Key 为偶数位十六进制字符。
密钥只在手机填写，不要写入仓库或构建配置。
[原项目配置获取说明](extract.md)

蓝牙操作一次只连接一个门禁；连接期间使用独立的门禁配置快照。
「开门指令已发送」表示蓝牙写入完成，实际门是否打开需要现场确认。

## 下载 APK

进入仓库 Actions，选择成功的 **Build installable APK** 运行，
下载 **SafeBaiyun-multi-door-apk**，解压后安装 **SafeBaiyun-multi-door.apk**。
这是已签名、可独立安装运行的 Debug APK，无需连接开发电脑。
支持 Android 5.0 及以上；原有小部件建议 Android 12 及以上使用。

CI 使用临时 Debug 签名，每次新构建可能使用不同证书；它也不保证与原作者 APK 签名相同。
如果覆盖安装提示签名不一致，请先自行保存所有门禁的 MAC 和 Key，再卸载旧版安装。
卸载会清除本地数据；此时无法自动迁移。长期发布应配置自己的固定 Release 签名。

## 构建与验证

使用 JDK 17、Android SDK 34、Gradle 8.4：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

输出：app/build/outputs/apk/debug/app-debug.apk。
已修复原仓库空的 Gradle Wrapper JAR；来源为 Gradle 官方 v8.4.0。
Actions 在 main 和 codex/** 分支 push、PR 以及手动触发时运行。
单元测试覆盖旧数据迁移、多门禁独立存储、编辑、默认切换、删除、
JSON 持久化及输入校验；CI 另检查 Lint、APK 签名和应用清单。
实机蓝牙连接与门禁开门需现场验证。
