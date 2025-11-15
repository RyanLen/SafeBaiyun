import Foundation
import CommonCrypto

/// DES加密服务，用于门禁通信的数据加密
class CryptoService {

    // MARK: - DES加密/解密

    /// DES加密方法
    /// - Parameters:
    ///   - data: 需要加密的数据
    ///   - key: 加密密钥（8字节）
    /// - Returns: 加密后的数据
    static func desEncrypt(data: Data, key: Data) -> Data? {
        // 确保密钥长度为8字节
        guard key.count == kCCKeySizeDES else {
            print("DES密钥长度必须为8字节")
            return nil
        }

        // 计算需要的缓冲区大小
        let bufferSize = data.count + kCCBlockSizeDES
        var buffer = Data(count: bufferSize)
        var numBytesEncrypted: size_t = 0

        let cryptStatus = buffer.withUnsafeMutableBytes { bufferBytes in
            data.withUnsafeBytes { dataBytes in
                key.withUnsafeBytes { keyBytes in
                    CCCrypt(
                        CCOperation(kCCEncrypt),              // 加密操作
                        CCAlgorithm(kCCAlgorithmDES),         // DES算法
                        CCOptions(kCCOptionECBMode),          // ECB模式，无填充
                        keyBytes.baseAddress,                  // 密钥
                        kCCKeySizeDES,                         // 密钥大小
                        nil,                                   // IV（ECB模式不需要）
                        dataBytes.baseAddress,                 // 输入数据
                        data.count,                           // 输入数据大小
                        bufferBytes.baseAddress,               // 输出缓冲区
                        bufferSize,                           // 输出缓冲区大小
                        &numBytesEncrypted                    // 实际加密的字节数
                    )
                }
            }
        }

        guard cryptStatus == kCCSuccess else {
            print("DES加密失败: \(cryptStatus)")
            return nil
        }

        buffer.removeSubrange(numBytesEncrypted..<buffer.count)
        return buffer
    }

    /// DES解密方法
    /// - Parameters:
    ///   - data: 需要解密的数据
    ///   - key: 解密密钥（8字节）
    /// - Returns: 解密后的数据
    static func desDecrypt(data: Data, key: Data) -> Data? {
        // 确保密钥长度为8字节
        guard key.count == kCCKeySizeDES else {
            print("DES密钥长度必须为8字节")
            return nil
        }

        let bufferSize = data.count + kCCBlockSizeDES
        var buffer = Data(count: bufferSize)
        var numBytesDecrypted: size_t = 0

        let cryptStatus = buffer.withUnsafeMutableBytes { bufferBytes in
            data.withUnsafeBytes { dataBytes in
                key.withUnsafeBytes { keyBytes in
                    CCCrypt(
                        CCOperation(kCCDecrypt),              // 解密操作
                        CCAlgorithm(kCCAlgorithmDES),         // DES算法
                        CCOptions(kCCOptionECBMode),          // ECB模式，无填充
                        keyBytes.baseAddress,                  // 密钥
                        kCCKeySizeDES,                         // 密钥大小
                        nil,                                   // IV（ECB模式不需要）
                        dataBytes.baseAddress,                 // 输入数据
                        data.count,                           // 输入数据大小
                        bufferBytes.baseAddress,               // 输出缓冲区
                        bufferSize,                           // 输出缓冲区大小
                        &numBytesDecrypted                    // 实际解密的字节数
                    )
                }
            }
        }

        guard cryptStatus == kCCSuccess else {
            print("DES解密失败: \(cryptStatus)")
            return nil
        }

        buffer.removeSubrange(numBytesDecrypted..<buffer.count)
        return buffer
    }
}