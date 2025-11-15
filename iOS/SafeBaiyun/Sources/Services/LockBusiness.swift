import Foundation

/// 门锁业务逻辑，处理门禁开锁的数据包构造和加密
class LockBusiness {

    // MARK: - 数据包构造

    /// 构造开门数据包
    /// - Parameters:
    ///   - readData: 从门禁读取的数据
    ///   - macAddress: 门禁MAC地址
    ///   - key: 加密密钥
    /// - Returns: 构造好的加密数据包
    static func buildUnlockPacket(readData: Data, macAddress: String, key: String) -> Data? {
        // 提取MAC地址后4字节
        let macBytes = extractMacBytes(from: macAddress)
        guard macBytes.count == 4 else {
            print("MAC地址格式错误")
            return nil
        }

        // 转换密钥为Data
        guard let keyData = hexStringToData(key) else {
            print("密钥格式错误")
            return nil
        }

        // 准备要加密的数据（根据Android版本逻辑）
        var inputData = Data()

        // 从读取的数据中提取需要的部分（通常是随机数）
        if readData.count >= 8 {
            inputData.append(readData[0..<8])
        } else {
            inputData.append(readData)
            // 填充到8字节
            while inputData.count < 8 {
                inputData.append(0x00)
            }
        }

        // DES加密
        guard let encryptedData = CryptoService.desEncrypt(data: inputData, key: keyData) else {
            print("数据加密失败")
            return nil
        }

        // 构造最终数据包
        var packet = Data()

        // 包头
        packet.append(0xA5)

        // 数据长度（加密数据 + MAC后4字节 + 固定字节）
        let dataLength = UInt8(encryptedData.count + 7)
        packet.append(dataLength)

        // 命令码
        packet.append(0x05)

        // MAC地址后4字节
        packet.append(contentsOf: macBytes)

        // 固定字节
        packet.append(contentsOf: [0x00, 0x01, 0x07])

        // 加密后的数据
        packet.append(encryptedData)

        // 计算校验和
        let checksum = calculateChecksum(packet)
        packet.append(checksum)

        // 包尾
        packet.append(0x5A)

        return packet
    }

    // MARK: - 辅助方法

    /// 从MAC地址字符串提取后4字节
    private static func extractMacBytes(from macAddress: String) -> Data {
        // 移除冒号和转换为大写
        let cleanMac = macAddress.replacingOccurrences(of: ":", with: "").uppercased()

        // 取后8个字符（4字节）
        guard cleanMac.count >= 8 else { return Data() }

        let lastChars = String(cleanMac.suffix(8))
        return hexStringToData(lastChars) ?? Data()
    }

    /// 十六进制字符串转Data
    private static func hexStringToData(_ hex: String) -> Data? {
        var data = Data()
        var hex = hex.replacingOccurrences(of: " ", with: "")

        // 确保是偶数长度
        if hex.count % 2 != 0 {
            hex = "0" + hex
        }

        var index = hex.startIndex
        while index < hex.endIndex {
            let nextIndex = hex.index(index, offsetBy: 2)
            let bytes = hex[index..<nextIndex]
            if let byte = UInt8(bytes, radix: 16) {
                data.append(byte)
            } else {
                return nil
            }
            index = nextIndex
        }
        return data
    }

    /// 计算校验和
    private static func calculateChecksum(_ data: Data) -> UInt8 {
        var sum: UInt32 = 0
        for byte in data {
            sum += UInt32(byte)
        }
        return UInt8(sum & 0xFF)
    }

    /// Data转十六进制字符串（用于调试）
    static func dataToHexString(_ data: Data) -> String {
        return data.map { String(format: "%02X", $0) }.joined(separator: " ")
    }
}