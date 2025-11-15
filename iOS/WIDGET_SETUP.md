# 🎯 iOS小组件配置指南

## 概述
小组件代码已经准备好，但需要在Xcode中手动添加Widget Extension Target。

## 📱 在Xcode中添加小组件

### 步骤 1：添加Widget Extension
1. 在Xcode中打开 `SafeBaiyun.xcodeproj`
2. 选择菜单：**File → New → Target...**
3. 选择 **"Widget Extension"**（在Application Extension分类下）
4. 点击 **Next**

### 步骤 2：配置Widget基本信息
在弹出的对话框中配置：
- **Product Name**: `SafeBaiyunWidget`
- **Team**: 选择您的开发团队（同主应用）
- **Bundle Identifier**: `cn.huacheng.safebaiyun.ios.widget`
- **Language**: Swift
- **Include Configuration Intent**: ❌ 不勾选
- **Project**: SafeBaiyun
- **Embed in Application**: SafeBaiyun

点击 **Finish**

### 步骤 3：替换Widget文件
Xcode会自动生成一些模板文件，我们需要用已有的文件替换它们：

1. 在Xcode中删除自动生成的文件：
   - 选中 `SafeBaiyunWidget` 组
   - 删除其中的所有.swift文件（保留Info.plist）

2. 添加我们的Widget文件：
   - 右键点击 `SafeBaiyunWidget` 组
   - 选择 **"Add Files to SafeBaiyun..."**
   - 导航到 `iOS/SafeBaiyunWidget/`
   - 选择 `SafeBaiyunWidget.swift`
   - 确保勾选了正确的Target（SafeBaiyunWidgetExtension）
   - 点击 **Add**

### 步骤 4：配置App Groups

#### 4.1 配置主应用
1. 选择 **SafeBaiyun** Target
2. 选择 **Signing & Capabilities** 标签
3. 点击 **"+ Capability"**
4. 双击 **"App Groups"**
5. 点击 **"+"** 添加新的App Group
6. 输入：`group.cn.huacheng.safebaiyun`
7. 确保勾选了这个App Group

#### 4.2 配置Widget Extension
1. 选择 **SafeBaiyunWidgetExtension** Target
2. 选择 **Signing & Capabilities** 标签
3. 点击 **"+ Capability"**
4. 双击 **"App Groups"**
5. 勾选同样的App Group：`group.cn.huacheng.safebaiyun`

### 步骤 5：修改主应用DataStore
主应用的DataStore需要支持App Groups数据共享。

确保 `DataStore.swift` 中使用了App Group：
```swift
private let appGroupIdentifier = "group.cn.huacheng.safebaiyun"

// 保存数据时同时写入App Group
if let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier) {
    sharedDefaults.set(macAddress, forKey: "macAddress")
}
```

### 步骤 6：编译运行
1. 选择目标设备（iPhone模拟器或真机）
2. 按 **Command + R** 运行应用
3. Widget Extension会自动包含在应用中

## 📲 在模拟器/真机上添加小组件

### 在模拟器上
1. 运行应用后，返回主屏幕
2. 长按主屏幕空白处
3. 点击左上角的 **"+"** 按钮
4. 搜索 **"平安白云"**
5. 选择想要的小组件尺寸：
   - 小尺寸（2x2）：快速开门按钮
   - 中等尺寸（4x2）：显示状态和开门按钮
   - 大尺寸（4x4）：完整界面
6. 点击 **"添加小组件"**

### 测试小组件
1. 点击小组件会启动应用并触发开门操作
2. 如果未配置，会提示需要先配置

## ⚠️ 常见问题

### 1. 小组件不显示
- 确保App Groups配置正确
- 重新编译并安装应用
- 在设置中重新添加小组件

### 2. 小组件显示"未配置"
- 打开主应用
- 进入设置页面
- 输入MAC地址和密钥
- 保存配置

### 3. 签名问题
- 确保Widget Extension使用同样的开发团队
- 检查Bundle Identifier是否正确

## 📁 文件结构
```
iOS/
├── SafeBaiyun/                    # 主应用
│   ├── SafeBaiyun.entitlements   # App Groups配置
│   └── ...
└── SafeBaiyunWidget/              # 小组件
    ├── SafeBaiyunWidget.swift    # 小组件代码
    ├── SafeBaiyunWidget.entitlements # App Groups配置
    └── Info.plist                 # 小组件配置
```

## ✅ 完成
按照以上步骤配置后，您的iOS应用就拥有完整的小组件功能了！

- 小组件支持三种尺寸
- 点击小组件快速开门
- 通过App Groups共享数据