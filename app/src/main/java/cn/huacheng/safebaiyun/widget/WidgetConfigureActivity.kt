package cn.huacheng.safebaiyun.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cn.huacheng.safebaiyun.MainActivity
import cn.huacheng.safebaiyun.theme.SafeBaiyunTheme
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.util.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_CANCELED, result)
        if (!WidgetBindingStore.isOwnWidget(this, widgetId)) {
            finish()
            return
        }
        setContent {
            SafeBaiyunTheme {
                val doors by DataRepo.state.collectAsState()
                val bindings by WidgetBindingStore.state.collectAsState()
                var saving by remember { mutableStateOf(false) }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("选择组件的门禁", style = MaterialTheme.typography.headlineSmall)
                        Text("每个桌面组件可选择不同门禁，点击下方门禁完成设置。")
                        if (doors.doors.isEmpty()) {
                            Text("还没有门禁，请先到 App 添加或导入，然后返回此页。")
                        }
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(doors.doors, key = { it.id }) { door ->
                                OutlinedButton(onClick = {
                                    saving = true
                                    lifecycleScope.launch {
                                        try {
                                            WidgetBindingStore.bind(widgetId, door.id)
                                            WidgetBindingStore.updateWidget(this@WidgetConfigureActivity, widgetId)
                                            setResult(RESULT_OK, result)
                                            finish()
                                        } catch (e: CancellationException) { throw e }
                                        catch (_: Exception) {
                                            saving = false
                                            showToast("组件设置失败，请重试")
                                        }
                                    }
                                }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                        Text(door.name)
                                        Text(door.mac, style = MaterialTheme.typography.bodySmall)
                                        if (bindings.doorsByWidget[widgetId] == door.id) Text("当前绑定")
                                    }
                                }
                            }
                        }
                        OutlinedButton(onClick = { startActivity(Intent(this@WidgetConfigureActivity, MainActivity::class.java)) },
                            enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("打开 App 管理门禁") }
                        TextButton(onClick = { finish() }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}
