package cn.huacheng.safebaiyun.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import cn.huacheng.safebaiyun.ShortcutActivity
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.util.ContextHolder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WidgetBindingStore {
    private val preferences by lazy {
        ContextHolder.get().getSharedPreferences("widget_bindings", Context.MODE_PRIVATE)
    }
    private val mutableState by lazy {
        val encoded = preferences.getString("bindings", null)
        MutableStateFlow(if (encoded == null) WidgetBindings() else
            runCatching { Json.decodeFromString<WidgetBindings>(encoded) }.getOrDefault(WidgetBindings()))
    }
    val state get() = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun save(value: WidgetBindings) {
        check(preferences.edit().putString("bindings", Json.encodeToString(value)).commit()) {
            "无法保存组件配置"
        }
        mutableState.value = value
    }

    @Synchronized
    fun bind(widgetId: Int, doorId: String) {
        require(DataRepo.state.value.doors.any { it.id == doorId }) { "门禁已被删除" }
        save(mutableState.value.bind(widgetId, doorId))
    }

    @Synchronized
    fun remove(ids: IntArray) = save(mutableState.value.remove(ids))

    @Synchronized
    fun restore(oldIds: IntArray, newIds: IntArray) {
        if (oldIds.size == newIds.size) save(mutableState.value.restore(oldIds, newIds))
    }

    fun isOwnWidget(context: Context, id: Int): Boolean {
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: return false
        return provider.packageName == context.packageName &&
            provider.className in listOf(MediumReceiver::class.java.name, LargeReceiver::class.java.name)
    }

    suspend fun updateWidget(context: Context, id: Int) {
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: return
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(id)
        when (provider.className) {
            MediumReceiver::class.java.name -> MediumWidget.update(context, glanceId)
            LargeReceiver::class.java.name -> LargeWidget.update(context, glanceId)
        }
    }

    fun refreshAll() {
        scope.launch {
            val context = ContextHolder.get()
            try { MediumWidget.updateAll(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            try { LargeWidget.updateAll(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
    }

    fun configureIntent(context: Context, widgetId: Int) =
        Intent(context, WidgetConfigureActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            data = Uri.parse("safebaiyun-widget://configure/$widgetId")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }

    fun unlockIntent(context: Context, widgetId: Int) =
        Intent(context, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            // Extras alone do not distinguish PendingIntents. Each widget needs a unique data URI.
            data = Uri.parse("safebaiyun-widget://unlock/$widgetId")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
}
