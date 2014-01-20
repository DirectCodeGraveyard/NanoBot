package org.nanobot.test

import org.junit.Test
import org.nanobot.StateContainer

import static org.junit.Assert.*

class StatesTest {
    private StateContainer states = new StateContainer()

    @Test
    void toggleTest() {
        states.toggle("toggle")
        assertTrue(states.has("toggle"))
    }

    @Test
    void onTest() {
        states.on("on")
        assertTrue(states.has("on"))
    }

    @Test
    void offTest() {
        states.on("off")
        states.off("off")
        assertFalse(states.has("off"))
    }

    @Test
    void currentTest() {
        toggleTest()
        onTest()
        offTest()
        assertArrayEquals([
                "toggle",
                "on"
        ].toArray(), states.current().toArray())
    }
}