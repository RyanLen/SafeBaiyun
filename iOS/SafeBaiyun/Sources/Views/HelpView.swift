import SwiftUI

struct HelpView: View {
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // 介绍
                    SectionView(title: "关于平安白云") {
                        Text("平安白云是一个离线蓝牙门禁开门应用，专为广州市白云区蓝牙门禁系统设计。无需网络连接即可快速开门。")
                            .font(.body)
                            .foregroundColor(.secondary)
                    }

                    // 使用步骤
                    SectionView(title: "使用步骤") {
                        VStack(alignment: .leading, spacing: 15) {
                            StepView(number: 1, title: "获取配置", description: "从官方应用数据库提取MAC地址和密钥")
                            StepView(number: 2, title: "配置应用", description: "在设置中输入MAC地址和密钥")
                            StepView(number: 3, title: "开启蓝牙", description: "确保手机蓝牙已开启")
                            StepView(number: 4, title: "一键开门", description: "靠近门禁，点击开门按钮即可")
                        }
                    }

                    // 数据提取
                    SectionView(title: "如何提取数据") {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("官方应用：com.huacheng.baiyunuser")
                                .font(.footnote)
                                .foregroundColor(.secondary)

                            Text("方法一：Root设备")
                                .font(.headline)
                                .padding(.top, 5)
                            Text("访问路径：/data/data/com.huacheng.baiyunuser/databases/*.db")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Text("方法二：无Root设备")
                                .font(.headline)
                                .padding(.top, 5)
                            Text("通过手机备份功能提取应用数据")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Text("数据库表：t_device")
                                .font(.headline)
                                .padding(.top, 5)
                            Text("• MAC_NUM: 门禁MAC地址\n• PRODUCT_KEY: 加密密钥")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    // 小组件说明
                    SectionView(title: "iOS小组件") {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("添加小组件到主屏幕，实现快速开门：")
                                .font(.body)

                            VStack(alignment: .leading, spacing: 8) {
                                WidgetFeature(icon: "square.grid.2x2", text: "小尺寸：快速开门按钮")
                                WidgetFeature(icon: "rectangle", text: "中等尺寸：显示状态和开门按钮")
                                WidgetFeature(icon: "rectangle.fill", text: "大尺寸：完整功能界面")
                            }

                            Text("长按主屏幕 → 添加小组件 → 选择平安白云")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .padding(.top, 5)
                        }
                    }

                    // 常见问题
                    SectionView(title: "常见问题") {
                        VStack(alignment: .leading, spacing: 15) {
                            FAQItem(
                                question: "开门失败怎么办？",
                                answer: "1. 检查蓝牙是否开启\n2. 确认MAC地址和密钥正确\n3. 靠近门禁设备重试"
                            )
                            FAQItem(
                                question: "是否需要网络连接？",
                                answer: "不需要，本应用完全离线运行"
                            )
                            FAQItem(
                                question: "数据安全吗？",
                                answer: "所有数据使用Keychain加密存储，仅在本设备使用"
                            )
                        }
                    }

                    // 版权信息
                    VStack(alignment: .center, spacing: 5) {
                        Text("版本 1.0")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("iOS原生版本")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                }
                .padding()
            }
            .navigationTitle("帮助")
            .navigationBarItems(
                trailing: Button("完成") {
                    presentationMode.wrappedValue.dismiss()
                }
            )
        }
    }
}

// MARK: - 子视图组件

struct SectionView<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.headline)
                .foregroundColor(.primary)
            content
        }
    }
}

struct StepView: View {
    let number: Int
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 15) {
            ZStack {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 30, height: 30)
                Text("\(number)")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

struct WidgetFeature: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundColor(.blue)
                .frame(width: 20)
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct FAQItem: View {
    let question: String
    let answer: String

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text("Q: \(question)")
                .font(.subheadline)
                .fontWeight(.medium)
            Text("A: \(answer)")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - 预览

struct HelpView_Previews: PreviewProvider {
    static var previews: some View {
        HelpView()
    }
}