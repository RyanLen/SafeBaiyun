package cn.huacheng.safebaiyun.unlock

import android.content.Context
import androidx.core.content.edit
import cn.huacheng.safebaiyun.util.ContextHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DataRepo {
    private val preferences by lazy {
        ContextHolder.get().getSharedPreferences("data", Context.MODE_PRIVATE)
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableState by lazy { MutableStateFlow(load()) }
    val state get() = mutableState.asStateFlow()

    private fun load(): DoorState {
        val encoded = preferences.getString("doors_v2", null)
        if (encoded != null) return json.decodeFromString<DoorState>(encoded)
        val migrated = migrateLegacy(
            preferences.getString("mac", "") ?: "",
            preferences.getString("key", "") ?: ""
        )
        persist(migrated)
        return migrated
    }

    private fun persist(value: DoorState) {
        preferences.edit {
            putString("doors_v2", json.encodeToString(value))
            remove("mac")
            remove("key")
        }
    }

    @Synchronized
    fun save(door: Door) = update(mutableState.value.upsert(door))

    @Synchronized
    fun remove(id: String) = update(mutableState.value.remove(id))

    @Synchronized
    fun setDefault(id: String) {
        require(mutableState.value.doors.any { it.id == id })
        update(mutableState.value.copy(defaultId = id))
    }

    private fun update(value: DoorState) {
        persist(value)
        mutableState.value = value
    }

    fun defaultDoor(): Door? = mutableState.value.defaultDoor()
    fun readData(): Pair<String, String> = defaultDoor()?.let { it.mac to it.key } ?: ("" to "")
}
