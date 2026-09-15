package cn.huacheng.safebaiyun.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cn.huacheng.safebaiyun.unlock.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(door: Door?, onDismiss: () -> Unit) {
    var name by rememberSaveable(door?.id) { mutableStateOf(door?.name ?: "") }
    var mac by rememberSaveable(door?.id) { mutableStateOf(door?.mac ?: "") }
    var key by rememberSaveable(door?.id) { mutableStateOf(door?.key ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (door == null) "添加门禁" else "编辑门禁", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("门禁名称") }, singleLine = true)
            OutlinedTextField(mac, { mac = it }, Modifier.fillMaxWidth(), label = { Text("MAC 地址") }, singleLine = true)
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("Key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隐藏" else "显示") } })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                error = doorValidationError(name, mac, key)
                if (error == null) {
                    try {
                        DataRepo.save(Door(door?.id ?: UUID.randomUUID().toString(), name, mac, key))
                        onDismiss()
                    } catch (e: IllegalArgumentException) {
                        error = e.message ?: "无法保存门禁"
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}
