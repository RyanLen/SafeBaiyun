import SwiftUI

@main
struct SafeBaiyunApp: App {
    @StateObject private var bluetoothManager = BluetoothManager()
    @StateObject private var dataStore = DataStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(bluetoothManager)
                .environmentObject(dataStore)
                .handleURLScheme()
        }
    }
}

// MARK: - App Configuration
extension SafeBaiyunApp {
    init() {
        // 配置应用启动时的设置
        setupAppearance()
    }

    private func setupAppearance() {
        // 设置导航栏外观
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemBackground

        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
    }
}