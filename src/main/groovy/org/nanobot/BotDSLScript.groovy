package org.nanobot

abstract class BotDSLScript extends Script {
    @Override
    Object invokeMethod(String name, Object args) {
        dsl.invokeMethod(name, args)
    }
}
