package cn.huacheng.safebaiyun.widget

import cn.huacheng.safebaiyun.unlock.Door
import cn.huacheng.safebaiyun.unlock.DoorState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WidgetBindingsTest {
    private val a = Door("a", "大门", "AA:BB:CC:DD:EE:01", "0011")
    private val b = Door("b", "单元门", "AA:BB:CC:DD:EE:02", "2233")
    private val doors = DoorState(listOf(a, b), a.id)

    @Test fun differentInstancesResolveDifferentDoors() {
        val bindings = WidgetBindings().bind(101, a.id).bind(102, b.id)
        assertEquals(a, bindings.resolve(101, doors))
        assertEquals(b, bindings.resolve(102, doors))
    }

    @Test fun changingDefaultDoesNotChangeEitherWidget() {
        val bindings = WidgetBindings().bind(101, a.id).bind(102, b.id)
        assertEquals(a, bindings.resolve(101, doors.copy(defaultId = b.id)))
        assertEquals(b, bindings.resolve(102, doors.copy(defaultId = b.id)))
    }

    @Test fun reconfiguringOneWidgetDoesNotAffectOthers() {
        val bindings = WidgetBindings().bind(101, a.id).bind(102, a.id).bind(101, b.id)
        assertEquals(b, bindings.resolve(101, doors))
        assertEquals(a, bindings.resolve(102, doors))
    }

    @Test fun deletedOrUnconfiguredDoorNeverFallsBackToDefault() {
        val bindings = WidgetBindings().bind(101, b.id)
        assertNull(bindings.resolve(101, doors.remove(b.id)))
        assertNull(bindings.resolve(999, doors))
        // Even a new door imported with the same MAC is not implicitly granted this widget binding.
        assertNull(bindings.resolve(101, DoorState(listOf(b.copy(id = "new-id")), "new-id")))
    }

    @Test fun editingBoundDoorUsesLatestNameAndKey() {
        val updated = b.copy(name = "新名称", key = "FFFF")
        val bindings = WidgetBindings().bind(101, b.id)
        assertEquals(updated, bindings.resolve(101, doors.upsert(updated)))
    }

    @Test fun deletingOneWidgetKeepsOtherBindings() {
        val bindings = WidgetBindings().bind(101, a.id).bind(102, b.id).remove(intArrayOf(101))
        assertNull(bindings.resolve(101, doors))
        assertEquals(b, bindings.resolve(102, doors))
    }

    @Test fun restoreUsesSnapshotWhenIdsOverlap() {
        val bindings = WidgetBindings().bind(101, a.id).bind(102, b.id).bind(999, a.id)
            .restore(intArrayOf(101, 102), intArrayOf(102, 103))
        assertNull(bindings.resolve(101, doors))
        assertEquals(a, bindings.resolve(102, doors))
        assertEquals(b, bindings.resolve(103, doors))
        assertEquals(a, bindings.resolve(999, doors))
    }

    @Test fun bindingsSurviveSerializationWithoutCredentials() {
        val bindings = WidgetBindings().bind(101, b.id)
        val encoded = Json.encodeToString(bindings)
        assertFalse(encoded.contains(b.mac))
        assertFalse(encoded.contains(b.key))
        assertEquals(bindings, Json.decodeFromString<WidgetBindings>(encoded))
    }
}
