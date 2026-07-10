# iOS 构建说明

本模板包含最小可打开的 `iosApp.xcodeproj`。首次在 Mac 上构建请按以下步骤：

## 1. 生成 shared CocoaPods 产物

在仓库根目录执行：

```bash
./gradlew :shared:generateDummyFramework
# 或完整编译
./gradlew :shared:podInstall
```

## 2. 安装 Pods

```bash
cd iosApp
pod install
```

## 3. 用 Xcode 打开

请打开 **`iosApp.xcworkspace`**（由 CocoaPods 生成），不要直接打开 `.xcodeproj`。

```bash
open iosApp.xcworkspace
```

## 4. 运行

选择模拟器或真机，运行 `iosApp` target。

## Flow 观察说明

`LoginViewModelBridge` 使用短间隔轮询同步 `StateFlow`，便于模板开箱运行。
正式项目建议接入 **KMP-NativeCoroutines** 或 **SKIE**。
