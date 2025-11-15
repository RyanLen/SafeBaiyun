import Foundation

// MARK: - Character扩展

extension Character {
    /// 判断字符是否为十六进制字符
    var isHexDigit: Bool {
        let hexCharacters = "0123456789ABCDEFabcdef"
        return hexCharacters.contains(self)
    }
}

// MARK: - String扩展

extension String {
    /// 移除字符串中的空格和冒号
    var cleanHexString: String {
        return self.replacingOccurrences(of: " ", with: "")
                   .replacingOccurrences(of: ":", with: "")
                   .replacingOccurrences(of: "-", with: "")
                   .uppercased()
    }

    /// 验证是否为有效的MAC地址格式
    var isValidMACAddress: Bool {
        let cleanMAC = self.cleanHexString
        return cleanMAC.count == 12 && cleanMAC.allSatisfy { $0.isHexDigit }
    }

    /// 格式化MAC地址（添加冒号）
    var formattedMACAddress: String {
        let clean = self.cleanHexString
        guard clean.count == 12 else { return self }

        var result = ""
        for (index, char) in clean.enumerated() {
            if index > 0 && index % 2 == 0 {
                result += ":"
            }
            result.append(char)
        }
        return result
    }

    /// 验证是否为有效的十六进制密钥
    func isValidHexKey(length: Int) -> Bool {
        let cleanKey = self.cleanHexString
        return cleanKey.count == length && cleanKey.allSatisfy { $0.isHexDigit }
    }
}

// MARK: - Data扩展

extension Data {
    /// 转换为十六进制字符串
    var hexString: String {
        return self.map { String(format: "%02X", $0) }.joined()
    }

    /// 转换为带空格的十六进制字符串（用于调试）
    var hexDebugString: String {
        return self.map { String(format: "%02X", $0) }.joined(separator: " ")
    }

    /// 从十六进制字符串初始化
    init?(hexString: String) {
        let clean = hexString.cleanHexString

        guard clean.count % 2 == 0 else { return nil }

        var data = Data()
        var index = clean.startIndex

        while index < clean.endIndex {
            let nextIndex = clean.index(index, offsetBy: 2)
            let byteString = clean[index..<nextIndex]

            guard let byte = UInt8(byteString, radix: 16) else { return nil }
            data.append(byte)

            index = nextIndex
        }

        self = data
    }
}