package org.nanobot

class BotManager {
    def ArrayList<NanoBot> bots = []

    def addBot(server, port, nick) {
        bots.add(new NanoBot(server, port, nick))
    }

    def connectAll() {
        bots.each {
            it.connect()
        }
    }

    def on(String name, Closure closure) {
        bots.each {
            it.on(name, closure)
        }
    }

    def join(channel) {
        bots.each {
            it.join(channel)
        }
    }

    def msg(target, String message) {
        bots.each {
            it.msg(target, message)
        }
    }

    def disconnectAll() {
        bots.each {
            it.disconnect()
        }
    }

    def part(channel) {
        bots.each {
            it.part(channel)
        }
    }
}
