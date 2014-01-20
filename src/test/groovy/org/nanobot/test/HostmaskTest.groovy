package org.nanobot.test

import org.junit.Test
import org.nanobot.NanoBot
import static org.junit.Assert.*

class HostmaskTest {
    @Test
    void parseNormalHostmask() {
        def mask = 'samrg472!~deathcraz@I.got.g-lined.cu.c'
        def expect = 'samrg472'
        def actual = NanoBot.parseNickname(mask)
        assertEquals(expect, actual)
    }

    @Test
    void parseSecondHostmask() {
        def mask = ':samrg472!~deathcraz@I.got.g-lined.cu.c'
        def expect = 'samrg472'
        def actual = NanoBot.parseNickname(mask)
        assertEquals(expect, actual)
    }
}
