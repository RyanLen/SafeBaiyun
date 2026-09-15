package cn.huacheng.safebaiyun.widget

import cn.huacheng.safebaiyun.unlock.Door
import cn.huacheng.safebaiyun.unlock.DoorState
import kotlinx.serialization.Serializable

@Serializable
data class WidgetBindings(val doorsByWidget: Map<Int, String> = emptyMap()) {
    fun bind(widgetId: Int, doorId: String): WidgetBindings {
        require(widgetId >= 0 && doorId.isNotBlank())
        return copy(doorsByWidget = doorsByWidget + (widgetId to doorId))
    }

    fun remove(widgetIds: IntArray) = copy(doorsByWidget = doorsByWidget - widgetIds.toSet())

    fun restore(oldIds: IntArray, newIds: IntArray): WidgetBindings {
        require(oldIds.size == newIds.size)
        val moved = oldIds.zip(newIds.toList()).mapNotNull { (old, new) ->
            doorsByWidget[old]?.let { new to it }
        }.toMap()
        return copy(doorsByWidget = (doorsByWidget - oldIds.toSet()) + moved)
    }

    // Deliberately never fall back to the default door, even after deletion/import.
    fun resolve(widgetId: Int, doors: DoorState): Door? =
        doorsByWidget[widgetId]?.let { id -> doors.doors.find { it.id == id } }
}
