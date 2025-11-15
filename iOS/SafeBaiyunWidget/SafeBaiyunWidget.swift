import WidgetKit
import SwiftUI

// MARK: - 小组件专用的简化DataStore

struct WidgetDataStore {
    // 注意：如果使用自动生成的App Group，请更新这里的ID
    // 格式通常是: group.com.yourteamid.SafeBaiyun
    static let appGroupIdentifier = "group.cn.huacheng.safebaiyun"

    static var isConfigured: Bool {
        guard let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier),
              let macAddress = sharedDefaults.string(forKey: "macAddress"),
              !macAddress.isEmpty else {
            return false
        }
        // 简化检查，只检查MAC地址是否存在
        return true
    }
}

// MARK: - 小组件提供者

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date(), isConfigured: false)
    }

    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> ()) {
        let entry = SimpleEntry(
            date: Date(),
            isConfigured: WidgetDataStore.isConfigured
        )
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let entry = SimpleEntry(
            date: Date(),
            isConfigured: WidgetDataStore.isConfigured
        )

        // 小组件不需要频繁更新，设置较长的更新间隔
        let timeline = Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(3600)))
        completion(timeline)
    }
}

// MARK: - 小组件条目

struct SimpleEntry: TimelineEntry {
    let date: Date
    let isConfigured: Bool
}

// MARK: - 小组件视图

struct SafeBaiyunWidgetEntryView : View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var widgetFamily

    var body: some View {
        switch widgetFamily {
        case .systemSmall:
            SmallWidgetView(isConfigured: entry.isConfigured)
                .containerBackground(.fill.tertiary, for: .widget)
        case .systemMedium:
            MediumWidgetView(isConfigured: entry.isConfigured)
                .containerBackground(.fill.tertiary, for: .widget)
        case .systemLarge:
            LargeWidgetView(isConfigured: entry.isConfigured)
                .containerBackground(.fill.tertiary, for: .widget)
        default:
            MediumWidgetView(isConfigured: entry.isConfigured)
                .containerBackground(.fill.tertiary, for: .widget)
        }
    }
}

// MARK: - 小尺寸小组件

struct SmallWidgetView: View {
    let isConfigured: Bool

    var body: some View {
        Link(destination: URL(string: "safebaiyun://unlock")!) {
            ZStack {
                // 背景
                Color.blue
                    .opacity(0.9)

                VStack(spacing: 8) {
                    Image(systemName: isConfigured ? "lock.open" : "lock")
                        .font(.system(size: 40))
                        .foregroundColor(.white)

                    Text("开门")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
            }
        }
    }
}

// MARK: - 中等尺寸小组件

struct MediumWidgetView: View {
    let isConfigured: Bool

    var body: some View {
        Link(destination: URL(string: "safebaiyun://unlock")!) {
            ZStack {
                // 背景
                LinearGradient(
                    gradient: Gradient(colors: [Color.blue.opacity(0.1), Color.white]),
                    startPoint: .top,
                    endPoint: .bottom
                )

                HStack(spacing: 20) {
                    // 左侧：标题和状态
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 5) {
                            Image(systemName: "lock.shield")
                                .font(.title2)
                                .foregroundColor(.blue)
                            Text("平安白云")
                                .font(.headline)
                                .foregroundColor(.primary)
                        }

                        Text(isConfigured ? "点击快速开门" : "请先配置应用")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    // 右侧：开门按钮
                    ZStack {
                        Circle()
                            .fill(LinearGradient(
                                gradient: Gradient(colors: [Color.blue, Color.blue.opacity(0.8)]),
                                startPoint: .top,
                                endPoint: .bottom
                            ))
                            .frame(width: 60, height: 60)

                        Image(systemName: isConfigured ? "lock.open" : "gear")
                            .font(.system(size: 30))
                            .foregroundColor(.white)
                    }
                }
                .padding()
            }
        }
    }
}

// MARK: - 大尺寸小组件

struct LargeWidgetView: View {
    let isConfigured: Bool

    var body: some View {
        Link(destination: URL(string: "safebaiyun://unlock")!) {
            ZStack {
                // 背景
                LinearGradient(
                    gradient: Gradient(colors: [Color.blue.opacity(0.1), Color.white]),
                    startPoint: .top,
                    endPoint: .bottom
                )

                VStack(spacing: 20) {
                    // 顶部：标题区域
                    HStack {
                        Image(systemName: "lock.shield")
                            .font(.largeTitle)
                            .foregroundColor(.blue)

                        VStack(alignment: .leading) {
                            Text("平安白云")
                                .font(.title2)
                                .fontWeight(.bold)
                            Text("蓝牙门禁系统")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()
                    }
                    .padding(.horizontal)

                    Spacer()

                    // 中间：开门按钮
                    ZStack {
                        Circle()
                            .fill(LinearGradient(
                                gradient: Gradient(colors: [Color.blue, Color.blue.opacity(0.7)]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ))
                            .frame(width: 100, height: 100)
                            .shadow(color: .blue.opacity(0.3), radius: 10, x: 0, y: 5)

                        VStack(spacing: 8) {
                            Image(systemName: isConfigured ? "lock.open" : "gear")
                                .font(.system(size: 50))
                                .foregroundColor(.white)

                            Text(isConfigured ? "开门" : "配置")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                    }

                    Spacer()

                    // 底部：状态提示
                    HStack {
                        Image(systemName: isConfigured ? "checkmark.circle" : "exclamationmark.circle")
                            .foregroundColor(isConfigured ? .green : .orange)

                        Text(isConfigured ? "已配置，点击开门" : "请先打开应用进行配置")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
                .padding(.vertical)
            }
        }
    }
}

// MARK: - 小组件主入口

@main
struct SafeBaiyunWidget: Widget {
    let kind: String = "SafeBaiyunWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            SafeBaiyunWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("平安白云")
        .description("快速开启蓝牙门禁")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - 预览

struct SafeBaiyunWidget_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SafeBaiyunWidgetEntryView(entry: SimpleEntry(date: Date(), isConfigured: true))
                .previewContext(WidgetPreviewContext(family: .systemSmall))
                .containerBackground(.fill.tertiary, for: .widget)

            SafeBaiyunWidgetEntryView(entry: SimpleEntry(date: Date(), isConfigured: true))
                .previewContext(WidgetPreviewContext(family: .systemMedium))
                .containerBackground(.fill.tertiary, for: .widget)

            SafeBaiyunWidgetEntryView(entry: SimpleEntry(date: Date(), isConfigured: true))
                .previewContext(WidgetPreviewContext(family: .systemLarge))
                .containerBackground(.fill.tertiary, for: .widget)
        }
    }
}