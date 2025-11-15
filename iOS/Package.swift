// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "SafeBaiyun",
    defaultLocalization: "zh-Hans",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "SafeBaiyun",
            targets: ["SafeBaiyun"]),
    ],
    dependencies: [
        // 依赖项可以在这里添加
    ],
    targets: [
        .target(
            name: "SafeBaiyun",
            dependencies: [],
            path: "SafeBaiyun/Sources",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "SafeBaiyunTests",
            dependencies: ["SafeBaiyun"],
            path: "SafeBaiyunTests"
        ),
    ]
)