# 平安白云 iOS版

基于Swift原生开发的iOS蓝牙门禁应用，支持离线开门和iOS小组件。

## 功能特性

- ✅ **完全离线**：无需网络连接即可开门
- ✅ **iOS小组件**：支持小、中、大三种尺寸的桌面小组件
- ✅ **快速开门**：一键开启蓝牙门禁
- ✅ **安全存储**：使用Keychain加密存储敏感数据
- ✅ **SwiftUI界面**：现代化的原生iOS体验
- ✅ **URL Scheme**：支持从小组件和快捷指令调用

## 技术架构

### 核心框架
- **开发语言**: Swift 5.9+
- **UI框架**: SwiftUI
- **蓝牙通信**: CoreBluetooth
- **小组件**: WidgetKit
- **加密**: CommonCrypto (DES)
- **数据存储**: Keychain + UserDefaults
- **最低支持**: iOS 14.0

### 项目结构
```
iOS/
├── SafeBaiyun/
│   ├── Sources/
│   │   ├── App/                  # 应用入口
│   │   ├── Views/                # UI视图
│   │   ├── Services/             # 核心服务
│   │   │   ├── BluetoothManager.swift  # 蓝牙管理
│   │   │   ├── CryptoService.swift     # 加密服务
│   │   │   ├── LockBusiness.swift      # 门锁业务逻辑
│   │   │   └── DataStore.swift         # 数据存储
│   │   └── Utils/                # 工具类
│   └── Info.plist                # 应用配置
├── SafeBaiyunWidget/             # 小组件扩展
│   ├── SafeBaiyunWidget.swift   # 小组件实现
│   └── Info.plist
└── Package.swift                 # Swift包配置
```

## 核心功能实现

### 1. 蓝牙通信
- 使用CoreBluetooth框架
- 服务UUID: `14839AC4-7D7E-415C-9A42-167340CF2339`
- 实现BLE连接、服务发现、特征读写

### 2. DES加密
- 使用CommonCrypto库
- DES/ECB/NoPadding模式
- 与Android版本保持一致的加密算法

### 3. 数据存储
- MAC地址: UserDefaults + App Groups (小组件共享)
- 加密密钥: Keychain (安全存储)

### 4. iOS小组件
- **小尺寸** (2x2): 快速开门按钮
- **中等尺寸** (4x2): 标题 + 开门按钮
- **大尺寸** (4x4): 完整界面展示

### 5. URL Scheme
- 协议: `safebaiyun://`
- 开门: `safebaiyun://unlock`
- 设置: `safebaiyun://settings`

## 使用说明

### 1. 获取配置数据
从官方应用数据库提取MAC地址和密钥：
- 官方应用: com.huacheng.baiyunuser
- 数据库表: t_device
- MAC_NUM: 门禁MAC地址
- PRODUCT_KEY: 加密密钥

### 2. 配置应用
1. 打开应用，点击"设置"
2. 输入MAC地址（格式: AA:BB:CC:DD:EE:FF）
3. 输入16位十六进制密钥
4. 保存配置

### 3. 添加小组件
1. 长按主屏幕进入编辑模式
2. 点击"+"添加小组件
3. 搜索"平安白云"
4. 选择合适的小组件尺寸
5. 添加到主屏幕

### 4. 开门操作
- **应用内**: 点击主界面的开门按钮
- **小组件**: 点击小组件直接开门
- **快捷指令**: 使用URL Scheme调用

## 编译运行

### 环境要求
- Xcode 15.0+
- iOS 14.0+
- Swift 5.9+

### 编译步骤
1. 打开Xcode
2. 选择 File → Open
3. 导航到 `iOS/` 目录
4. 打开 `Package.swift` 或创建Xcode项目
5. 选择目标设备
6. Command + R 运行

### 配置App Groups（小组件数据共享）
1. 在Xcode中选择项目
2. 选择主应用Target
3. Capabilities → App Groups
4. 添加: `group.cn.huacheng.safebaiyun`
5. 对Widget Extension重复相同操作

## 安全说明

- 使用DES加密算法保护通信数据
- 密钥使用Keychain安全存储
- 仅能开启用户有权访问的门禁
- 建议启用Face ID/Touch ID额外保护

## 与Android版本对比

| 功能 | Android版 | iOS版 |
|------|----------|-------|
| 蓝牙开门 | ✅ | ✅ |
| 桌面小组件 | ✅ (2种) | ✅ (3种) |
| 快捷方式 | ✅ | ✅ (URL Scheme) |
| 加密算法 | DES | DES |
| 数据存储 | SharedPreferences | Keychain |
| UI框架 | Jetpack Compose | SwiftUI |
| 最低版本 | Android 5.0 | iOS 14.0 |

## 后续优化

- [ ] 添加Siri快捷指令支持
- [ ] 实现Apple Watch应用
- [ ] 支持Face ID/Touch ID验证
- [ ] 添加开门历史记录
- [ ] 支持多个门禁配置
- [ ] 优化蓝牙连接稳定性
- [ ] 添加通知中心快捷操作

## 许可说明

本项目仅供学习研究使用，通过逆向工程实现了门禁系统的离线开门功能。