# Interview Assistant

Kotlin Multiplatform 面试助手，首期支持 Android 与 Windows Desktop。

## 已实现

- 多简历导入、PaddleOCR 异步解析、任务恢复
- Windows WASAPI 系统回环音频与 Android 麦克风采集
- 讯飞 WebAPI 流式听写、动态修正与 55 秒自动换连
- 可配置 OpenAI 兼容接口，默认预设 `deepseek-v4-flash`
- 手动/自动触发回答、SSE 流式显示、取消与重新生成
- SQLDelight 本地历史、原简历私有存储、无录音落盘
- Android Keystore / Windows DPAPI 凭据保护
- 模拟面试占位模块

## 运行

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :desktopApp:run
.\gradlew.bat :desktopApp:packageMsi
```

首次启动后，在“设置”中填写 PaddleOCR、讯飞和大模型凭据并保存。凭据不会写入源码或业务数据库。

## 测试

```powershell
.\gradlew.bat :shared:desktopTest :shared:testDebugUnitTest
```

设置页提供真实 Provider 连接测试。测试调用需要用户自己的有效凭据，可能产生少量模型调用费用。

## 数据位置

- Android：应用私有目录与 Android Keystore
- Windows：`%APPDATA%\InterviewAssistant` 与当前用户 DPAPI

业务数据库与简历内容一期未做数据库级加密；卸载或清理数据前请按需备份。
