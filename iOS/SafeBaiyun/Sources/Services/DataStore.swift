import Foundation
import Security

/// 数据存储管理器，使用Keychain和UserDefaults
class DataStore: ObservableObject {

    // MARK: - 发布属性

    @Published var macAddress: String = ""
    @Published var encryptionKey: String = ""
    @Published var isConfigured: Bool = false

    // MARK: - 常量

    private let macAddressKey = "cn.huacheng.safebaiyun.macAddress"
    private let encryptionKeyKey = "cn.huacheng.safebaiyun.encryptionKey"
    private let appGroupIdentifier = "group.cn.huacheng.safebaiyun"

    // MARK: - 初始化

    init() {
        loadConfiguration()
    }

    // MARK: - 公共方法

    /// 保存配置
    func saveConfiguration() {
        // 保存MAC地址到UserDefaults（非敏感数据）
        if let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier) {
            sharedDefaults.set(macAddress, forKey: "macAddress")
        }
        UserDefaults.standard.set(macAddress, forKey: "macAddress")

        // 保存加密密钥到Keychain（敏感数据）
        saveToKeychain(key: encryptionKeyKey, value: encryptionKey)

        // 更新配置状态
        updateConfigurationStatus()
    }

    /// 加载配置
    func loadConfiguration() {
        // 从UserDefaults加载MAC地址
        if let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier),
           let mac = sharedDefaults.string(forKey: "macAddress") {
            macAddress = mac
        } else if let mac = UserDefaults.standard.string(forKey: "macAddress") {
            macAddress = mac
        }

        // 从Keychain加载加密密钥
        encryptionKey = loadFromKeychain(key: encryptionKeyKey) ?? ""

        // 更新配置状态
        updateConfigurationStatus()
    }

    /// 清除配置
    func clearConfiguration() {
        // 清除UserDefaults
        if let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier) {
            sharedDefaults.removeObject(forKey: "macAddress")
        }
        UserDefaults.standard.removeObject(forKey: "macAddress")

        // 清除Keychain
        deleteFromKeychain(key: encryptionKeyKey)

        // 重置属性
        macAddress = ""
        encryptionKey = ""
        isConfigured = false
    }

    // MARK: - Keychain操作

    /// 保存到Keychain
    private func saveToKeychain(key: String, value: String) {
        // 先删除旧值
        deleteFromKeychain(key: key)

        // 准备数据
        guard let data = value.data(using: .utf8) else { return }

        // 创建查询字典
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: Bundle.main.bundleIdentifier ?? "cn.huacheng.safebaiyun",
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecAttrSynchronizable as String: false
        ]

        // 添加到Keychain
        let status = SecItemAdd(query as CFDictionary, nil)
        if status != errSecSuccess {
            print("Keychain保存失败: \(status)")
        }
    }

    /// 从Keychain加载
    private func loadFromKeychain(key: String) -> String? {
        // 创建查询字典
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: Bundle.main.bundleIdentifier ?? "cn.huacheng.safebaiyun",
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        // 查询Keychain
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        // 处理结果
        if status == errSecSuccess,
           let data = result as? Data,
           let value = String(data: data, encoding: .utf8) {
            return value
        }

        return nil
    }

    /// 从Keychain删除
    private func deleteFromKeychain(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: Bundle.main.bundleIdentifier ?? "cn.huacheng.safebaiyun"
        ]

        SecItemDelete(query as CFDictionary)
    }

    // MARK: - 私有方法

    /// 更新配置状态
    private func updateConfigurationStatus() {
        isConfigured = !macAddress.isEmpty && !encryptionKey.isEmpty
    }
}

// MARK: - 用于Widget的扩展

extension DataStore {

    /// 从共享容器加载配置（供Widget使用）
    static func loadSharedConfiguration() -> (macAddress: String, encryptionKey: String)? {
        let store = DataStore()

        // 尝试从App Group加载
        if let sharedDefaults = UserDefaults(suiteName: store.appGroupIdentifier),
           let mac = sharedDefaults.string(forKey: "macAddress"),
           !mac.isEmpty {
            // 从Keychain加载密钥
            let key = store.loadFromKeychain(key: store.encryptionKeyKey) ?? ""
            if !key.isEmpty {
                return (mac, key)
            }
        }

        return nil
    }

    /// 检查是否已配置（供Widget使用）
    static var isConfiguredForWidget: Bool {
        return loadSharedConfiguration() != nil
    }
}