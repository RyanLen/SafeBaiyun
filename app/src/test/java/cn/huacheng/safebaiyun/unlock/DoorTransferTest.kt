package cn.huacheng.safebaiyun.unlock

import org.junit.Assert.*
import org.junit.Test

class DoorTransferTest {
    private val first = Door("local-id", "小区大门", "AA:BB:CC:DD:EE:01", "0011223344556677")
    private val second = Door("second-id", "单元门", "AA:BB:CC:DD:EE:02", "8899AABBCCDDEEFF")
    private fun shared(door: Door) = SharedDoor(door.name, door.mac, door.key)
    private fun rejects(text: String) {
        try { DoorTransfer.parse(text); fail("Expected validation failure") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun exportedConfigRoundTripsWithoutLocalIdsOrDefault() {
        val encoded = DoorTransfer.export(listOf(first, second))
        assertFalse(encoded.contains("local-id"))
        assertFalse(encoded.contains("defaultId"))
        assertTrue(encoded.contains("safebaiyun.doors"))
        assertEquals(listOf(shared(first), shared(second)), DoorTransfer.parse(encoded))
    }

    @Test fun acceptsSingleDoorAndArrayAndMarkdownFence() {
        val item = """{"name":"门","mac":"aa:bb:cc:dd:ee:01","key":"00AA"}"""
        assertEquals(first.mac, DoorTransfer.parse(item).single().mac)
        assertEquals(1, DoorTransfer.parse("[$item]").size)
        assertEquals(1, DoorTransfer.parse("```json\n$item\n```").size)
    }

    @Test fun validatesSchemaVersionAndRequiredFields() {
        rejects("""{"format":"other","version":1,"doors":[]}""")
        rejects(DoorTransfer.export(listOf(first)).replace("\"version\": 1", "\"version\": 2"))
        rejects("""{"name":"门","mac":"AA:BB:CC:DD:EE:01"}""")
        rejects("""{"name":42,"mac":"AA:BB:CC:DD:EE:01","key":"00AA"}""")
        rejects("[]")
        rejects("not json")
        rejects("null")
    }

    @Test fun duplicateMacWithinPayloadIsRejected() {
        val item = """{"name":"门","mac":"AA:BB:CC:DD:EE:01","key":"00AA"}"""
        rejects("[$item,$item]")
    }

    @Test fun defaultImportSkipsExistingAndAddsWithFreshIdentity() {
        val existing = DoorState().upsert(first)
        val result = DoorTransfer.merge(existing, listOf(shared(first.copy(key = "FFFF")), shared(second)), false)
        assertEquals(1, result.added)
        assertEquals(1, result.skipped)
        assertEquals(0, result.updated)
        assertEquals(first, result.state.defaultDoor())
        assertEquals(first.key, result.state.doors[0].key)
        assertNotEquals(second.id, result.state.doors[1].id)
    }

    @Test fun explicitOverwritePreservesIdentityAndDefaultSelection() {
        val original = DoorState().upsert(first).upsert(second).copy(defaultId = second.id)
        val result = DoorTransfer.merge(original, listOf(shared(second.copy(name = "新名称", key = "FFFF"))), true)
        assertEquals(1, result.updated)
        assertEquals(second.id, result.state.defaultDoor()?.id)
        assertEquals("FFFF", result.state.defaultDoor()?.key)
        assertEquals(first, result.state.doors[0])
        assertEquals(2, result.state.doors.size)
    }

    @Test fun invalidBatchDoesNotChangeOriginalState() {
        val original = DoorState().upsert(first)
        try {
            DoorTransfer.merge(original, listOf(shared(second), SharedDoor("坏配置", "BAD", "GG")), true)
            fail("Expected validation failure")
        } catch (_: IllegalArgumentException) { }
        assertEquals(listOf(first), original.doors)
        assertEquals(first.id, original.defaultId)
    }

    @Test fun importingIntoEmptyStateChoosesFirstDoorAsDefault() {
        val result = DoorTransfer.merge(DoorState(), listOf(shared(first), shared(second)), false)
        assertEquals(first.mac, result.state.defaultDoor()?.mac)
        assertEquals(2, result.added)
    }

    @Test fun enforcesTextCountAndFieldLimits() {
        rejects(" ".repeat(DoorTransfer.MAX_TEXT_LENGTH + 1))
        val item = """{"name":"门","mac":"AA:BB:CC:DD:EE:01","key":"00AA"}"""
        rejects("[" + List(101) { item }.joinToString(",") + "]")
        rejects(item.replace("00AA", "AA".repeat(513)))
        rejects(item.replace("门", "门".repeat(101)))
    }

    @Test fun errorMessagesDoNotExposeSecrets() {
        val secret = "DEADBEEF"
        try {
            DoorTransfer.parse("""{"name":"门","mac":"AA:BB:CC:DD:EE:01","key":"$secret" garbage}""")
            fail("Expected validation failure")
        } catch (e: IllegalArgumentException) {
            assertFalse(e.message.orEmpty().contains(secret))
        }
    }

    @Test fun emptyOrInvalidConfigCannotBeExported() {
        try { DoorTransfer.export(emptyList()); fail("Expected validation failure") }
        catch (_: IllegalArgumentException) { }
        try { DoorTransfer.export(listOf(first.copy(key = "bad"))); fail("Expected validation failure") }
        catch (_: IllegalArgumentException) { }
    }
}
