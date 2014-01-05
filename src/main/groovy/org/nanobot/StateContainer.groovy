package org.nanobot

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

    void reset() {
        states.clear()
    }

    Set<String> current() {
        return states.findAll {
            it.value
        }.keySet().asImmutable()
    }
}