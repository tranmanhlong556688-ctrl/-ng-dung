package vn.minh.lgirremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LgAcProtocolTest {
    @Test fun knownOffCommandHasValidChecksum() = assertTrue(LgAcProtocol.isValidChecksum(LgAcProtocol.OFF_COMMAND))
    @Test fun encodesKnownCool19MediumFrame() = assertEquals(0x8800426, LgAcProtocol.encodeState(LgAcProtocol.State(true,19,LgAcProtocol.Mode.COOL,LgAcProtocol.Fan.MEDIUM)))
    @Test fun encodesKnownCool22LowestFrame() = assertEquals(0x8800707, LgAcProtocol.encodeState(LgAcProtocol.State(true,22,LgAcProtocol.Mode.COOL,LgAcProtocol.Fan.LOWEST)))
    @Test fun generatedPatternStaysWithinAndroidLimit() {
        val p = LgAcProtocol.profiles.first()
        val pattern = LgAcProtocol.buildPattern(LgAcProtocol.encodeState(LgAcProtocol.State(power=true)), p.timings)
        assertEquals(63, pattern.size)
        assertTrue(pattern.sum() < 2_000_000)
    }
}
