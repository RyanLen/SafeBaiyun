import SwiftUI

struct SettingsView: View {
    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject var dataStore: DataStore
    @State private var macAddress = ""
    @State private var encryptionKey = ""
    @State private var showingSaveAlert = false
    @State private var showingClearAlert = false

    var body: some View {
        NavigationView {
            Form {
                // MAC地址设置
                Section(header: Text("门禁设备")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("MAC地址")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("例如: AA:BB:CC:DD:EE:FF", text: $macAddress)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                    }
                    .padding(.vertical, 4)
                }

                // 密钥设置
                Section(header: Text("加密密钥")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("密钥（16位十六进制）")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("例如: 1234567890ABCDEF", text: $encryptionKey)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                    }
                    .padding(.vertical, 4)
                }

                // 说明
                Section(header: Text("说明")) {
                    VStack(alignment: .leading, spacing: 10) {
                        HelpItem(
                            icon: "info.circle",
                            text: "MAC地址和密钥需要从官方应用数据库中提取"
                        )
                        HelpItem(
                            icon: "lock.shield",
                            text: "数据使用Keychain安全存储，仅在本设备使用"
                        )
                        HelpItem(
                            icon: "antenna.radiowaves.left.and.right",
                            text: "确保蓝牙已开启且靠近门禁设备"
                        )
                    }
                }

                // 操作按钮
                Section {
                    Button(action: saveSettings) {
                        HStack {
                            Image(systemName: "checkmark.circle")
                            Text("保存设置")
                        }
                        .foregroundColor(.blue)
                    }

                    Button(action: { showingClearAlert = true }) {
                        HStack {
                            Image(systemName: "trash")
                            Text("清除数据")
                        }
                        .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("设置")
            .navigationBarItems(
                trailing: Button("完成") {
                    presentationMode.wrappedValue.dismiss()
                }
            )
            .alert(isPresented: $showingSaveAlert) {
                Alert(
                    title: Text("保存成功"),
                    message: Text("配置已安全保存"),
                    dismissButton: .default(Text("确定"))
                )
            }
            .alert(isPresented: $showingClearAlert) {
                Alert(
                    title: Text("确认清除"),
                    message: Text("将删除所有保存的配置数据"),
                    primaryButton: .destructive(Text("清除")) {
                        clearSettings()
                    },
                    secondaryButton: .cancel(Text("取消"))
                )
            }
        }
        .onAppear {
            loadSettings()
        }
    }

    // MARK: - 方法

    private func loadSettings() {
        macAddress = dataStore.macAddress
        encryptionKey = dataStore.encryptionKey
    }

    private func saveSettings() {
        // 验证MAC地址格式
        let cleanMac = macAddress.replacingOccurrences(of: ":", with: "")
            .replacingOccurrences(of: "-", with: "")
            .uppercased()

        guard cleanMac.count == 12,
              cleanMac.allSatisfy({ $0.isHexDigit }) else {
            // 这里应该显示错误提示
            return
        }

        // 验证密钥格式
        let cleanKey = encryptionKey.replacingOccurrences(of: " ", with: "").uppercased()
        guard cleanKey.count == 16,
              cleanKey.allSatisfy({ $0.isHexDigit }) else {
            // 这里应该显示错误提示
            return
        }

        // 保存设置
        dataStore.macAddress = macAddress.uppercased()
        dataStore.encryptionKey = cleanKey
        dataStore.saveConfiguration()

        showingSaveAlert = true
    }

    private func clearSettings() {
        macAddress = ""
        encryptionKey = ""
        dataStore.clearConfiguration()
    }
}

struct HelpItem: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.blue)
                .font(.system(size: 20))
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

// MARK: - 预览

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView()
            .environmentObject(DataStore())
    }
}