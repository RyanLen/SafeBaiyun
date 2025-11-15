import Foundation
import CoreBluetooth
import Combine

/// 蓝牙管理器，负责与门禁设备的BLE通信
class BluetoothManager: NSObject, ObservableObject {

    // MARK: - 常量

    // 门禁服务UUID（与Android版本保持一致）
    private let MAGIC_SERVICE_UUID = CBUUID(string: "14839AC4-7D7E-415C-9A42-167340CF2339")

    // MARK: - 发布属性

    @Published var isBluetoothEnabled = false
    @Published var isConnecting = false
    @Published var isConnected = false
    @Published var unlockStatus: UnlockStatus = .idle
    @Published var errorMessage: String?
    @Published var discoveredDevices: [CBPeripheral] = []

    // MARK: - 私有属性

    private var centralManager: CBCentralManager!
    private var currentPeripheral: CBPeripheral?
    private var targetService: CBService?
    private var readCharacteristic: CBCharacteristic?
    private var writeCharacteristic: CBCharacteristic?

    private var macAddress: String = ""
    private var encryptionKey: String = ""
    private var readData: Data?

    private var connectionTimer: Timer?
    private var unlockCompletion: ((Bool, String?) -> Void)?

    // MARK: - 解锁状态

    enum UnlockStatus: String {
        case idle = "空闲"
        case connecting = "连接中..."
        case connected = "已连接"
        case reading = "读取数据中..."
        case writing = "发送开门指令..."
        case success = "开门成功"
        case failed = "开门失败"
    }

    // MARK: - 初始化

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: .main)
    }

    // MARK: - 公共方法

    /// 开始扫描蓝牙设备
    func startScanning() {
        guard isBluetoothEnabled else {
            errorMessage = "蓝牙未开启"
            return
        }

        discoveredDevices.removeAll()
        centralManager.scanForPeripherals(withServices: nil, options: [
            CBCentralManagerScanOptionAllowDuplicatesKey: false
        ])

        // 10秒后自动停止扫描
        DispatchQueue.main.asyncAfter(deadline: .now() + 10) {
            self.stopScanning()
        }
    }

    /// 停止扫描
    func stopScanning() {
        centralManager.stopScan()
    }

    /// 执行开门操作
    /// - Parameters:
    ///   - macAddress: 门禁MAC地址
    ///   - key: 加密密钥
    ///   - completion: 完成回调
    func unlock(macAddress: String, key: String, completion: @escaping (Bool, String?) -> Void) {
        self.macAddress = macAddress.uppercased()
        self.encryptionKey = key
        self.unlockCompletion = completion

        // 重置状态
        unlockStatus = .connecting
        errorMessage = nil

        // 开始扫描目标设备
        centralManager.scanForPeripherals(withServices: [MAGIC_SERVICE_UUID], options: nil)

        // 设置超时定时器（10秒）
        connectionTimer = Timer.scheduledTimer(withTimeInterval: 10, repeats: false) { _ in
            self.handleTimeout()
        }
    }

    /// 断开连接
    func disconnect() {
        if let peripheral = currentPeripheral {
            centralManager.cancelPeripheralConnection(peripheral)
        }
        cleanup()
    }

    // MARK: - 私有方法

    private func handleTimeout() {
        unlockStatus = .failed
        errorMessage = "连接超时"
        unlockCompletion?(false, "连接超时")
        disconnect()
    }

    private func cleanup() {
        connectionTimer?.invalidate()
        connectionTimer = nil
        currentPeripheral = nil
        targetService = nil
        readCharacteristic = nil
        writeCharacteristic = nil
        readData = nil
        isConnecting = false
        isConnected = false
        unlockStatus = .idle
    }

    /// 处理服务发现
    private func handleService(_ service: CBService) {
        targetService = service
        currentPeripheral?.discoverCharacteristics(nil, for: service)
    }

    /// 处理特征发现
    private func handleCharacteristics(_ characteristics: [CBCharacteristic]) {
        for characteristic in characteristics {
            let properties = characteristic.properties

            // 查找可读特征
            if properties.contains(.read) {
                readCharacteristic = characteristic
                print("找到可读特征: \(characteristic.uuid)")
            }

            // 查找可写特征
            if properties.contains(.write) || properties.contains(.writeWithoutResponse) {
                writeCharacteristic = characteristic
                print("找到可写特征: \(characteristic.uuid)")
            }

            // 处理通知特征
            if properties.contains(.notify) || properties.contains(.indicate) {
                currentPeripheral?.setNotifyValue(true, for: characteristic)
                print("订阅通知特征: \(characteristic.uuid)")
            }
        }

        // 开始读取数据
        if let readChar = readCharacteristic {
            unlockStatus = .reading
            currentPeripheral?.readValue(for: readChar)
        } else if writeCharacteristic != nil {
            // 如果没有可读特征，直接尝试写入
            performUnlock()
        }
    }

    /// 执行开锁操作
    private func performUnlock() {
        guard let writeChar = writeCharacteristic else {
            unlockStatus = .failed
            errorMessage = "未找到可写特征"
            unlockCompletion?(false, "未找到可写特征")
            disconnect()
            return
        }

        // 构造开门数据包
        let dataToUse = readData ?? Data([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
        guard let packet = LockBusiness.buildUnlockPacket(
            readData: dataToUse,
            macAddress: macAddress,
            key: encryptionKey
        ) else {
            unlockStatus = .failed
            errorMessage = "数据包构造失败"
            unlockCompletion?(false, "数据包构造失败")
            disconnect()
            return
        }

        print("发送开门指令: \(LockBusiness.dataToHexString(packet))")
        unlockStatus = .writing
        currentPeripheral?.writeValue(packet, for: writeChar, type: .withResponse)
    }
}

// MARK: - CBCentralManagerDelegate

extension BluetoothManager: CBCentralManagerDelegate {

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            isBluetoothEnabled = true
            print("蓝牙已开启")
        case .poweredOff:
            isBluetoothEnabled = false
            errorMessage = "蓝牙已关闭"
        case .unauthorized:
            isBluetoothEnabled = false
            errorMessage = "蓝牙权限未授权"
        case .unsupported:
            isBluetoothEnabled = false
            errorMessage = "设备不支持蓝牙"
        default:
            isBluetoothEnabled = false
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                       advertisementData: [String : Any], rssi RSSI: NSNumber) {
        // 检查是否是目标设备
        if let peripheralName = peripheral.name {
            print("发现设备: \(peripheralName), RSSI: \(RSSI)")
        }

        // 检查MAC地址匹配（如果有广播数据中包含）
        let deviceAddress = peripheral.identifier.uuidString

        // 添加到发现列表（用于调试或选择设备）
        if !discoveredDevices.contains(where: { $0.identifier == peripheral.identifier }) {
            discoveredDevices.append(peripheral)
        }

        // 如果正在执行开锁操作，连接第一个发现的设备
        if unlockStatus == .connecting && currentPeripheral == nil {
            currentPeripheral = peripheral
            peripheral.delegate = self
            isConnecting = true
            centralManager.stopScan()
            centralManager.connect(peripheral, options: nil)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("已连接设备: \(peripheral.name ?? "Unknown")")
        isConnecting = false
        isConnected = true
        unlockStatus = .connected

        // 发现服务
        peripheral.discoverServices([MAGIC_SERVICE_UUID])
    }

    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("连接失败: \(error?.localizedDescription ?? "Unknown error")")
        unlockStatus = .failed
        errorMessage = error?.localizedDescription ?? "连接失败"
        unlockCompletion?(false, errorMessage)
        cleanup()
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        print("设备断开连接")
        cleanup()
    }
}

// MARK: - CBPeripheralDelegate

extension BluetoothManager: CBPeripheralDelegate {

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error {
            print("服务发现失败: \(error.localizedDescription)")
            unlockStatus = .failed
            errorMessage = "服务发现失败"
            unlockCompletion?(false, errorMessage)
            disconnect()
            return
        }

        guard let services = peripheral.services else {
            print("未发现任何服务")
            unlockStatus = .failed
            errorMessage = "未发现门禁服务"
            unlockCompletion?(false, errorMessage)
            disconnect()
            return
        }

        // 查找目标服务
        for service in services {
            if service.uuid == MAGIC_SERVICE_UUID {
                print("找到门禁服务")
                handleService(service)
                return
            }
        }

        // 如果没找到目标服务，尝试使用第一个服务
        if let firstService = services.first {
            print("使用默认服务: \(firstService.uuid)")
            handleService(firstService)
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if let error = error {
            print("特征发现失败: \(error.localizedDescription)")
            unlockStatus = .failed
            errorMessage = "特征发现失败"
            unlockCompletion?(false, errorMessage)
            disconnect()
            return
        }

        guard let characteristics = service.characteristics else {
            print("未发现任何特征")
            unlockStatus = .failed
            errorMessage = "未发现可用特征"
            unlockCompletion?(false, errorMessage)
            disconnect()
            return
        }

        handleCharacteristics(characteristics)
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if let error = error {
            print("读取数据失败: \(error.localizedDescription)")
            // 即使读取失败也尝试继续
            performUnlock()
            return
        }

        if let data = characteristic.value {
            print("读取到数据: \(LockBusiness.dataToHexString(data))")
            readData = data
            performUnlock()
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        connectionTimer?.invalidate()

        if let error = error {
            print("写入数据失败: \(error.localizedDescription)")
            unlockStatus = .failed
            errorMessage = "发送开门指令失败"
            unlockCompletion?(false, errorMessage)
        } else {
            print("开门指令发送成功")
            unlockStatus = .success
            unlockCompletion?(true, nil)
        }

        // 延迟断开连接
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.disconnect()
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        if let error = error {
            print("订阅通知失败: \(error.localizedDescription)")
        } else {
            print("已订阅特征通知: \(characteristic.uuid)")
        }
    }
}