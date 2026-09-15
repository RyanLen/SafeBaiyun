package cn.huacheng.safebaiyun.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.huacheng.safebaiyun.unlock.*
import cn.huacheng.safebaiyun.util.showToast

@Composable
fun MainView(navController: NavHostController) {
    val context = LocalContext.current
    val state by DataRepo.state.collectAsState()
    val busy by UnlockRepo.busy.collectAsState()
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Door?>(null) }
    var deleting by remember { mutableStateOf<Door?>(null) }
    var importing by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf<List<Door>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        showToast(if (it) "蓝牙权限已授予，请再次点击开门" else "开门需要附近设备权限")
    }
    Column(Modifier.fillMaxSize()) {
        MainTopBar(onEditClick = { editing = null; editorOpen = true },
            onHelperClick = { navController.navigate("helper") },
            onImportClick = { importing = true },
            onExportClick = { exporting = state.doors },
            canExport = state.doors.isNotEmpty())
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("我的门禁（${state.doors.size}）", style = MaterialTheme.typography.titleLarge)
                Text("快捷方式使用默认门禁；桌面组件可分别绑定门禁。", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { importing = true }) { Text("导入门禁") }
            }
            if (state.doors.isEmpty()) item {
                Text("还没有门禁，点击下方按钮添加。")
            }
            items(state.doors, key = { it.id }) { door ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(door.name, style = MaterialTheme.typography.titleMedium)
                        Text(door.mac, style = MaterialTheme.typography.bodyMedium)
                        if (state.defaultDoor()?.id == door.id) Text("默认门禁", color = MaterialTheme.colorScheme.primary)
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else UnlockRepo.unlock(door)
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (busy) "正在连接门禁…" else "开门")
                        }
                        OutlinedButton(onClick = { exporting = listOf(door) },
                            modifier = Modifier.fillMaxWidth()) { Text("导出分享") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { DataRepo.setDefault(door.id) },
                                enabled = state.defaultDoor()?.id != door.id) { Text("设为默认") }
                            TextButton(onClick = { editing = door; editorOpen = true }) { Text("编辑") }
                            TextButton(onClick = { deleting = door }) { Text("删除") }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = { editing = null; editorOpen = true },
                modifier = Modifier.fillMaxWidth()) { Text("添加门禁") } }
        }
    }
    if (importing) ImportDoorsDialog { importing = false }
    exporting?.let { doors -> ExportDoorsDialog(doors) { exporting = null } }
    if (editorOpen) EditDialog(editing) { editorOpen = false }
    deleting?.let { door ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除门禁？") },
            text = { Text("确定删除「${door.name}」？删除后需重新填写 MAC 和 Key。") },
            confirmButton = { TextButton(onClick = { DataRepo.remove(door.id); deleting = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
}
