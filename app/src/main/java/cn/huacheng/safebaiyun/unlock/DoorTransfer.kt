package cn.huacheng.safebaiyun.unlock

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class SharedDoor(val name: String, val mac: String, val key: String)

@Serializable
private data class DoorShare(val format: String, val version: Int, val doors: List<SharedDoor>)

data class DoorImportResult(val state: DoorState, val added: Int, val updated: Int, val skipped: Int)

object DoorTransfer {
    const val MAX_TEXT_LENGTH = 65536
    const val MAX_DOORS = 100
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private const val FORMAT = "safebaiyun.doors"

    fun export(doors: List<Door>): String {
        val shared = validate(doors.map { SharedDoor(it.name, it.mac, it.key) })
        val text = json.encodeToString(DoorShare(FORMAT, 1, shared))
        require(text.length <= MAX_TEXT_LENGTH) { "内容过长，请分批导出" }
        return text
    }

    fun parse(text: String): List<SharedDoor> {
        require(text.length <= MAX_TEXT_LENGTH) { "内容过长，最多支持 64K 字符" }
        var clean = text.trim().removePrefix("\uFEFF").trim()
        if (clean.startsWith("```json") && clean.endsWith("```")) {
            clean = clean.removePrefix("```json").removeSuffix("```").trim()
        } else if (clean.startsWith("```") && clean.endsWith("```")) {
            clean = clean.removePrefix("```").removeSuffix("```").trim()
        }
        require(clean.isNotEmpty()) { "请先粘贴门禁 JSON" }
        // Never expose serializer errors: they can contain the original secret text.
        val element = try { json.parseToJsonElement(clean) }
            catch (_: IllegalArgumentException) { throw IllegalArgumentException("JSON 格式错误，请复制完整内容") }
        val doors = try {
            when {
                element is JsonArray -> json.decodeFromJsonElement<List<SharedDoor>>(element)
                element is JsonObject && "doors" in element -> {
                    val share = json.decodeFromJsonElement<DoorShare>(element)
                    require(share.format == FORMAT) { "不支持的门禁配置格式" }
                    require(share.version == 1) { "配置版本不支持，请升级 App" }
                    share.doors
                }
                element is JsonObject -> listOf(json.decodeFromJsonElement<SharedDoor>(element))
                else -> throw IllegalArgumentException("需要门禁 JSON 对象或数组")
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            throw IllegalArgumentException("配置缺少必填字段，或字段类型不正确")
        }
        return validate(doors)
    }

    private fun validate(doors: List<SharedDoor>): List<SharedDoor> {
        require(doors.isNotEmpty()) { "配置中没有门禁" }
        require(doors.size <= MAX_DOORS) { "每次最多支持 100 个门禁" }
        val seen = mutableSetOf<String>()
        return doors.mapIndexed { index, door ->
            val error = doorValidationError(door.name, door.mac, door.key)
            require(error == null) { "第 ${index + 1} 个门禁：$error" }
            require(door.name.trim().length <= 100 && door.key.trim().length <= 1024) {
                "第 ${index + 1} 个门禁：名称或 Key 过长"
            }
            val normalized = door.copy(name = door.name.trim(), mac = normalizeMac(door.mac), key = door.key.trim())
            require(seen.add(normalized.mac)) { "配置中存在重复 MAC，请保留每个门禁的一份配置" }
            normalized
        }
    }

    /** Validate first, compute a complete new state, then let the repository persist once. */
    fun merge(current: DoorState, doors: List<SharedDoor>, replaceExisting: Boolean): DoorImportResult {
        val valid = validate(doors)
        var result = current
        var added = 0
        var updated = 0
        var skipped = 0
        for (door in valid) {
            val existing = result.doors.find { normalizeMac(it.mac) == door.mac }
            if (existing != null && !replaceExisting) {
                skipped++
            } else {
                result = result.upsert(Door(existing?.id ?: UUID.randomUUID().toString(), door.name, door.mac, door.key))
                if (existing == null) added++ else updated++
            }
        }
        return DoorImportResult(result, added, updated, skipped)
    }
}
