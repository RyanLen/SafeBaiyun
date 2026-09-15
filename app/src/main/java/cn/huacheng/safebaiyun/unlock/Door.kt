package cn.huacheng.safebaiyun.unlock

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class Door(val id: String, val name: String, val mac: String, val key: String)

@Serializable
data class DoorState(val doors: List<Door> = emptyList(), val defaultId: String? = null) {
    fun defaultDoor(): Door? = doors.find { it.id == defaultId } ?: doors.firstOrNull()

    fun upsert(door: Door): DoorState {
        require(door.id.isNotBlank())
        require(doorValidationError(door.name, door.mac, door.key) == null)
        require(doors.none { it.id != door.id && normalizeMac(it.mac) == normalizeMac(door.mac) }) {
            "这个 MAC 地址已经添加"
        }
        val normalized = door.copy(name = door.name.trim(), mac = normalizeMac(door.mac), key = door.key.trim())
        val updated = if (doors.any { it.id == door.id }) {
            doors.map { if (it.id == door.id) normalized else it }
        } else doors + normalized
        return DoorState(updated, defaultDoor()?.id ?: door.id)
    }

    fun remove(id: String): DoorState {
        val remaining = doors.filterNot { it.id == id }
        return DoorState(remaining, defaultId?.takeIf { selected -> remaining.any { it.id == selected } }
            ?: remaining.firstOrNull()?.id)
    }
}

fun normalizeMac(mac: String): String = mac.trim().uppercase(Locale.ROOT)

fun doorValidationError(name: String, mac: String, key: String): String? = when {
    name.trim().isEmpty() -> "请输入门禁名称"
    !Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}").matches(normalizeMac(mac)) ->
        "MAC 格式应为 AA:BB:CC:DD:EE:FF"
    key.trim().isEmpty() || key.trim().length % 2 != 0 ||
        !Regex("[0-9a-fA-F]+").matches(key.trim()) -> "Key 必须为偶数位十六进制字符"
    else -> null
}

/** Preserve even incomplete legacy credentials so users can correct them in the editor. */
fun migrateLegacy(mac: String, key: String): DoorState =
    if (mac.isBlank() && key.isBlank()) DoorState()
    else DoorState(listOf(Door("legacy", "原有门禁", normalizeMac(mac), key.trim())), "legacy")
