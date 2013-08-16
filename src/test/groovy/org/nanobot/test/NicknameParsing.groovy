package org.nanobot.test

import org.junit.Test
import org.nanobot.NanoBot
import static org.junit.Assert.*

class NicknameParsing {
    @Test
    void parseHostmask() {
        def mask = 'samrg472!my.mom.is.on.twitter.panicbnc.us'
        def expect = 'samrg472'
        def actual = NanoBot.getNick(mask)
        assertEquals(expect, actual)
    }
}
