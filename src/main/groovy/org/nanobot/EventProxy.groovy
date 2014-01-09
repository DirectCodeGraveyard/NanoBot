package org.nanobot

import groovy.transform.Canonical

@Canonical
class EventProxy {
    NanoBot bot

    /**
     * Retrieves a List of Handlers, which may be appended to
     */
    @Override
    List<Closure> getProperty(String name) {
        return bot.handlers[name]
    }
}