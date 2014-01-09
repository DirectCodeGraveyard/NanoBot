package org.nanobot

/**
 * Holds basic on/off statuses for different parts of a bot
 */
class StateContainer {
    private final Map<String, Boolean> states = [:].withDefault { false }

    void on(String name) {
        states[name] = true
    }

    void off(String name) {
        states[name] = false
    }

    boolean has(String name) {
        return states[name]
    }

    void toggle(String name) {
        states[name] = !states[name]
    }

    void reset() {
        states.clear()
    }

    Set<String> current() {
        return states.findAll {
            it.value
        }.keySet().asImmutable() // Immutable to prevent people from modifying states in bulk
    }
}