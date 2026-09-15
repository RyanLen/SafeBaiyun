package cn.huacheng.safebaiyun.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver

abstract class DoorWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetBindingStore.remove(appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        WidgetBindingStore.restore(oldWidgetIds, newWidgetIds)
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        WidgetBindingStore.refreshAll()
    }
}
