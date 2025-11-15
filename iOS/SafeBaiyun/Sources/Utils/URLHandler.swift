import Foundation
import SwiftUI

/// URL Scheme处理器，处理小组件和快捷指令的调用
class URLHandler: ObservableObject {

    // MARK: - 发布属性

    @Published var shouldUnlock = false
    @Published var shouldOpenSettings = false

    // MARK: - 单例

    static let shared = URLHandler()

    // MARK: - 方法

    /// 处理URL
    func handle(_ url: URL) {
        guard url.scheme == "safebaiyun" else { return }

        switch url.host {
        case "unlock":
            // 触发开门操作
            DispatchQueue.main.async {
                self.shouldUnlock = true
            }

        case "settings":
            // 打开设置界面
            DispatchQueue.main.async {
                self.shouldOpenSettings = true
            }

        default:
            break
        }
    }

    /// 重置状态
    func reset() {
        shouldUnlock = false
        shouldOpenSettings = false
    }
}

// MARK: - App修改以支持URL Scheme

extension SafeBaiyunApp {
    func setupURLHandling() {
        // 在SwiftUI App中处理URL
    }
}

// MARK: - View修改器，用于响应URL事件

struct URLHandlerModifier: ViewModifier {
    @StateObject private var urlHandler = URLHandler.shared
    @EnvironmentObject var bluetoothManager: BluetoothManager
    @EnvironmentObject var dataStore: DataStore
    @State private var showingSettingsFromURL = false

    func body(content: Content) -> some View {
        content
            .onOpenURL { url in
                urlHandler.handle(url)
            }
            .onChange(of: urlHandler.shouldUnlock) { shouldUnlock in
                if shouldUnlock {
                    performUnlockFromURL()
                    urlHandler.reset()
                }
            }
            .onChange(of: urlHandler.shouldOpenSettings) { shouldOpen in
                if shouldOpen {
                    showingSettingsFromURL = true
                    urlHandler.reset()
                }
            }
            .sheet(isPresented: $showingSettingsFromURL) {
                SettingsView()
                    .environmentObject(dataStore)
            }
    }

    private func performUnlockFromURL() {
        // 检查配置
        guard dataStore.isConfigured else {
            print("未配置，无法开门")
            return
        }

        // 检查蓝牙
        guard bluetoothManager.isBluetoothEnabled else {
            print("蓝牙未开启")
            return
        }

        // 执行开门
        bluetoothManager.unlock(
            macAddress: dataStore.macAddress,
            key: dataStore.encryptionKey
        ) { success, error in
            if success {
                print("通过URL Scheme开门成功")
                // 提供触觉反馈
                let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
                impactFeedback.impactOccurred()
            } else {
                print("通过URL Scheme开门失败: \(error ?? "未知错误")")
            }
        }
    }
}

// MARK: - View扩展

extension View {
    func handleURLScheme() -> some View {
        self.modifier(URLHandlerModifier())
    }
}