package cn.huacheng.safebaiyun.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.huacheng.safebaiyun.unlock.*
import cn.huacheng.safebaiyun.util.showToast

@Composable
fun ExportDoorsDialog(doors: List<Door>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    fun export(action: (String) -> Unit) {
        try {
            action(DoorTransfer.export(doors))
            onDismiss()
        } catch (e: IllegalArgumentException) {
            error = e.message ?: "无法导出配置"
        } catch (_: Exception) {
            error = "无法复制或打开分享，请重试"
        }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("导出 ${doors.size} 个门禁") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("JSON 包含可用于开门的 MAC 和 Key，且未加密。请只分享给可信的人，勿发到公开群聊或网站。")
                LazyColumn(Modifier.heightIn(max = 160.dp)) {
                    items(doors, key = { it.id }) { Text(it.name + " · " + it.mac) }
                }
                Text("接收方：打开 App → 导入门禁 → 粘贴 JSON → 预览后确认。")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                export { value ->
                    val clip = ClipData.newPlainText("门禁配置（含密钥）", value)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        clip.description.extras = PersistableBundle().apply {
                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                        }
                    }
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    if (Build.VERSION.SDK_INT < 33) showToast("门禁 JSON 已复制，请谨慎分享")
                }
            }) { Text("复制 JSON") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    export { value ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, value)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享门禁配置（含密钥）"))
                    }
                }) { Text("系统分享") }
            }
        })
}

@Composable
fun ImportDoorsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val current by DataRepo.state.collectAsState()
    // Credentials are intentionally not stored in rememberSaveable / saved instance state.
    var text by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<List<SharedDoor>?>(null) }
    var replace by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val preview = remember(current, parsed, replace) {
        parsed?.let { DoorTransfer.merge(current, it, replace) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("导入门禁", style = MaterialTheme.typography.titleLarge)
                Text("粘贴可信来源的门禁 JSON，预览并确认后才会保存。")
                if (parsed == null) {
                    OutlinedTextField(value = text, onValueChange = {
                        if (it.length <= DoorTransfer.MAX_TEXT_LENGTH) { text = it; error = null }
                        else error = "内容过长，最多支持 64K 字符"
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 160.dp),
                        label = { Text("门禁 JSON") })
                    TextButton(onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            val value = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text else null
                            if (value.isNullOrBlank()) error = "剪贴板没有文本，请手动粘贴"
                            else if (value.length > DoorTransfer.MAX_TEXT_LENGTH) error = "内容过长，最多支持 64K 字符"
                            else { text = value.toString(); error = null }
                        } catch (_: Exception) { error = "无法读取剪贴板，请长按输入框粘贴" }
                    }) { Text("从剪贴板粘贴") }
                } else {
                    Text("新增 ${preview?.added ?: 0} · 覆盖 ${preview?.updated ?: 0} · 跳过 ${preview?.skipped ?: 0}")
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(parsed!!, key = { it.mac }) { door ->
                            val exists = current.doors.any { normalizeMac(it.mac) == door.mac }
                            Column {
                                Text(door.name, style = MaterialTheme.typography.titleSmall)
                                Text(door.mac, style = MaterialTheme.typography.bodySmall)
                                Text(if (!exists) "新增" else if (replace) "覆盖名称和 Key" else "已存在，将跳过",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("覆盖相同 MAC 的门禁", modifier = Modifier.weight(1f))
                        Switch(checked = replace, onCheckedChange = { replace = it })
                    }
                    Text("默认跳过已存在的门禁；覆盖会替换名称和 Key，保留原默认门禁。", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { parsed = null; error = null }) { Text("返回修改 JSON") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        try {
                            if (parsed == null) {
                                parsed = DoorTransfer.parse(text)
                                error = null
                            } else {
                                val result = DataRepo.importDoors(parsed!!, replace)
                                showToast("已新增 ${result.added} 个、覆盖 ${result.updated} 个、跳过 ${result.skipped} 个")
                                onDismiss()
                            }
                        } catch (e: IllegalArgumentException) {
                            error = e.message ?: "无法导入配置"
                        }
                    }, enabled = parsed == null || (preview?.added ?: 0) + (preview?.updated ?: 0) > 0) {
                        Text(if (parsed == null) "预览导入" else "确认导入")
                    }
                }
            }
        }
    }
}
