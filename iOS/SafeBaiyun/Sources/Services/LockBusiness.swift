import Foundation

/// 门锁业务逻辑，处理门禁开锁的数据包构造和加密
class LockBusiness {

    // MARK: - 数据包构造（与Android版本完全一致）

    /// 构造开门数据包
    /// - Parameters:
    ///   - readData: 从门禁读取的数据
    ///   - macAddress: 门禁MAC地址
    ///   - key: 加密密钥
    /// - Returns: 构造好的加密数据包
    static func buildUnlockPacket(readData: Data, macAddress: String, key: String) -> Data? {
        // 将MAC地址转换为字节数组
        guard let macBytes = hexStringToBytes(macAddress.replacingOccurrences(of: ":", with: "")) else {
            print("MAC地址格式错误")
            return nil
        }

        // 确保MAC地址是6字节
        guard macBytes.count == 6 else {
            print("MAC地址长度错误: \(macBytes.count)")
            return nil
        }

        // 提取MAC地址的第2-5字节（索引2-5）作为header
        let headerBytesSubset = Data([macBytes[2], macBytes[3], macBytes[4], macBytes[5]])

        // 转换密钥为Data
        guard let keyData = hexStringToData(key) else {
            print("密钥格式错误")
            return nil
        }

        // 计算输入数据和密钥的校验和
        var sum = 0
        for byte in readData {
            sum += Int(byte)
        }
        for byte in keyData {
            sum += Int(byte)
        }

        // 构造要加密的数据
        let sumBytes = Data([UInt8(sum & 0xFF), UInt8((sum >> 8) & 0xFF)])

        // 组合校验和与输入数据
        var dataToEncrypt = Data()
        dataToEncrypt.append(sumBytes)
        dataToEncrypt.append(readData)

        // 填充到8字节的倍数
        let paddingNeeded = (8 - (dataToEncrypt.count % 8)) % 8
        if paddingNeeded > 0 {
            dataToEncrypt.append(Data(repeating: 0, count: paddingNeeded))
        }

        print("Sum: \(sum)")
        print("Before encryption: \(dataToHexString(dataToEncrypt))")

        // DES加密（只取前8字节加密结果）
        guard let encryptedData = CryptoService.desEncrypt(data: dataToEncrypt, key: keyData),
              encryptedData.count >= 8 else {
            print("数据加密失败")
            return nil
        }

        // 取前8字节的加密结果
        let encryptedBlock = encryptedData.prefix(8)

        print("After encryption: \(dataToHexString(Data(encryptedBlock)))")

        // 构造最终数据包
        var packet = Data()

        // 包头
        packet.append(0xA5) // -91的无符号表示

        // 数据长度（加密数据8字节 + 其他字段12字节 = 20）
        let dataLength = UInt8(20)
        packet.append(dataLength)

        // 命令码
        packet.append(0x05)

        // MAC地址的第2-5字节
        packet.append(headerBytesSubset)

        // 固定字节
        packet.append(contentsOf: [0x00, 0x01, 0x07])

        // 加密后的8字节数据
        packet.append(encryptedBlock)

        // 计算整个数据包的校验和
        var checksum = 0
        for byte in packet {
            checksum += Int(byte)
        }
        // 添加校验和占位符和包尾
        packet.append(0x00) // 校验和占位
        packet.append(0x5A) // 包尾（90）

        // 重新计算包含包尾的校验和
        checksum += 0x5A
        // 设置校验和（反码的低8位）
        packet[packet.count - 2] = UInt8((~checksum) & 0xFF)

        print("Final packet: \(dataToHexString(packet))")

        return packet
    }

    // MARK: - 辅助方法

    /// 十六进制字符串转字节数组
    private static func hexStringToBytes(_ hex: String) -> Data? {
        var data = Data()
        var hex = hex.replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: ":", with: "")
            .uppercased()

        // 确保是偶数长度
        if hex.count % 2 != 0 {
            return nil
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

    /// 十六进制字符串转Data（密钥专用）
    private static func hexStringToData(_ hex: String) -> Data? {
        return hexStringToBytes(hex)
    }

    /// Data转十六进制字符串（用于调试）
    static func dataToHexString(_ data: Data) -> String {
        return data.map { String(format: "%02X", $0) }.joined(separator: " ")
    }
}