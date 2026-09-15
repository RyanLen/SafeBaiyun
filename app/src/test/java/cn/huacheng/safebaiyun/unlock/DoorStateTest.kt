package cn.huacheng.safebaiyun.unlock

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DoorStateTest {
    private val first = Door("a", "大门", "AA:BB:CC:DD:EE:01", "0011223344556677")
    private val second = Door("b", "楼下", "AA:BB:CC:DD:EE:02", "8899AABBCCDDEEFF")

    @Test fun legacyCredentialsArePreserved() {
        val migrated = migrateLegacy("aa:bb:cc:dd:ee:01", first.key)
        assertEquals(first.mac, migrated.defaultDoor()?.mac)
        assertEquals(first.key, migrated.defaultDoor()?.key)
        assertEquals(1, migrated.doors.size)
        assertTrue(migrateLegacy("", "").doors.isEmpty())
    }

    @Test fun incompleteLegacyDataIsNotDiscarded() {
        assertEquals("broken", migrateLegacy("", "broken").defaultDoor()?.key)
    }

    @Test fun eachDoorKeepsItsOwnCredentialsAndDefaultIsStable() {
        val state = DoorState().upsert(first).upsert(second)
        assertEquals(first, state.defaultDoor())
        assertEquals(second, state.doors[1])
        val edited = state.upsert(second.copy(name = "单元门", key = "AABB"))
        assertEquals(first, edited.doors[0])
        assertEquals("AABB", edited.doors[1].key)
        assertEquals(2, edited.doors.size)
    }

    @Test fun deletingDefaultSelectsRemainingDoorAndLastDeleteClearsIt() {
        val state = DoorState().upsert(first).upsert(second).copy(defaultId = second.id)
        assertEquals(second, state.defaultDoor())
        assertEquals(first, state.remove(second.id).defaultDoor())
        val empty = state.remove(second.id).remove(first.id)
        assertNull(empty.defaultId)
        assertNull(empty.defaultDoor())
    }

    @Test fun persistenceRoundTripPreservesSelectionAndKeys() {
        val state = DoorState().upsert(first).upsert(second).copy(defaultId = second.id)
        assertEquals(state, Json.decodeFromString<DoorState>(Json.encodeToString(state)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateMacIsRejectedRegardlessOfCaseAndWhitespace() {
        DoorState().upsert(first).upsert(second.copy(mac = " aa:bb:cc:dd:ee:01 "))
    }

    @Test fun inputValidationRejectsMalformedCredentials() {
        assertNotNull(doorValidationError("", first.mac, first.key))
        assertNotNull(doorValidationError("门", "invalid", first.key))
        assertNotNull(doorValidationError("门", first.mac, "123"))
        assertNotNull(doorValidationError("门", first.mac, "GG"))
        assertNotNull(doorValidationError("门", first.mac, ""))
        assertNull(doorValidationError("门", " aa:bb:cc:dd:ee:01 ", " aabb "))
    }

    @Test fun missingDefaultFallsBackToFirstDoor() {
        assertEquals(first, DoorState(listOf(first, second), "missing").defaultDoor())
    }
}
