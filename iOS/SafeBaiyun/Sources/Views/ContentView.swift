import SwiftUI

struct ContentView: View {
    @EnvironmentObject var bluetoothManager: BluetoothManager
    @EnvironmentObject var dataStore: DataStore
    @State private var isSettingsPresented = false
    @State private var isHelpPresented = false
    @State private var showingAlert = false
    @State private var alertMessage = ""
    @State private var isUnlocking = false

    var body: some View {
        NavigationView {
            ZStack {
                // 背景渐变
                LinearGradient(
                    gradient: Gradient(colors: [Color.blue.opacity(0.1), Color.white]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                VStack(spacing: 30) {
                    // Logo和标题
                    VStack(spacing: 10) {
                        Image(systemName: "lock.shield")
                            .font(.system(size: 80))
                            .foregroundColor(.blue)

                        Text("平安白云")
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text("蓝牙门禁系统")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 50)

                    Spacer()

                    // 开门按钮
                    UnlockButton(isUnlocking: $isUnlocking) {
                        performUnlock()
                    }

                    // 状态显示
                    StatusView(status: bluetoothManager.unlockStatus)

                    Spacer()

                    // 底部按钮组
                    HStack(spacing: 40) {
                        BottomButton(
                            icon: "gear",
                            title: "设置",
                            action: { isSettingsPresented = true }
                        )

                        BottomButton(
                            icon: "questionmark.circle",
                            title: "帮助",
                            action: { isHelpPresented = true }
                        )
                    }
                    .padding(.bottom, 30)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $isSettingsPresented) {
                SettingsView()
                    .environmentObject(dataStore)
            }
            .sheet(isPresented: $isHelpPresented) {
                HelpView()
            }
            .alert(isPresented: $showingAlert) {
                Alert(
                    title: Text("提示"),
                    message: Text(alertMessage),
                    dismissButton: .default(Text("确定"))
                )
            }
        }
        .onAppear {
            checkBluetoothStatus()
        }
    }

    // MARK: - 方法

    private func performUnlock() {
        // 检查配置
        guard dataStore.isConfigured else {
            alertMessage = "请先在设置中配置MAC地址和密钥"
            showingAlert = true
            return
        }

        // 检查蓝牙状态
        guard bluetoothManager.isBluetoothEnabled else {
            alertMessage = "请开启蓝牙"
            showingAlert = true
            return
        }

        // 执行开锁
        isUnlocking = true
        bluetoothManager.unlock(
            macAddress: dataStore.macAddress,
            key: dataStore.encryptionKey
        ) { success, error in
            DispatchQueue.main.async {
                self.isUnlocking = false
                if success {
                    // 成功反馈
                    self.provideFeedback()
                } else {
                    self.alertMessage = error ?? "开门失败"
                    self.showingAlert = true
                }
            }
        }
    }

    private func checkBluetoothStatus() {
        if !bluetoothManager.isBluetoothEnabled {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                self.alertMessage = "请在系统设置中开启蓝牙"
                self.showingAlert = true
            }
        }
    }

    private func provideFeedback() {
        // 触觉反馈
        let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
        impactFeedback.impactOccurred()
    }
}

// MARK: - 子视图组件

struct UnlockButton: View {
    @Binding var isUnlocking: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [Color.blue, Color.blue.opacity(0.7)]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 150, height: 150)
                    .shadow(color: .blue.opacity(0.3), radius: 10, x: 0, y: 5)

                if isUnlocking {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(2)
                } else {
                    VStack(spacing: 10) {
                        Image(systemName: "lock.open")
                            .font(.system(size: 50))
                            .foregroundColor(.white)

                        Text("开门")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                }
            }
        }
        .disabled(isUnlocking)
        .scaleEffect(isUnlocking ? 0.95 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isUnlocking)
    }
}

struct StatusView: View {
    let status: BluetoothManager.UnlockStatus

    var body: some View {
        HStack {
            if status != .idle {
                Image(systemName: statusIcon)
                    .foregroundColor(statusColor)
                Text(status.rawValue)
                    .foregroundColor(statusColor)
                    .font(.subheadline)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(
            status != .idle ? Color.gray.opacity(0.1) : Color.clear
        )
        .cornerRadius(20)
        .animation(.easeInOut, value: status)
    }

    private var statusIcon: String {
        switch status {
        case .idle:
            return ""
        case .connecting, .connected:
            return "antenna.radiowaves.left.and.right"
        case .reading, .writing:
            return "arrow.up.arrow.down"
        case .success:
            return "checkmark.circle"
        case .failed:
            return "xmark.circle"
        }
    }

    private var statusColor: Color {
        switch status {
        case .success:
            return .green
        case .failed:
            return .red
        default:
            return .blue
        }
    }
}

struct BottomButton: View {
    let icon: String
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                Text(title)
                    .font(.caption)
            }
            .foregroundColor(.blue)
        }
    }
}

// MARK: - 预览

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(BluetoothManager())
            .environmentObject(DataStore())
    }
}