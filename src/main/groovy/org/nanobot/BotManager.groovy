package org.nanobot

class BotManager {
    def ArrayList<NanoBot> bots = []

    def addBot(server, port, nick) {
        def bot = new NanoBot(server, port, nick)
        bots.add(bot)
        return bot
    }

    def cloneBot(NanoBot original, nickname) {
        def bot = addBot(original.server, original.port, nickname)
        bot.handlers = original.handlers
        bot.userName = original.userName
        bot.realName = original.realName
        bot.commandPrefix = original.commandPrefix
        return bot
    }

    def connectAll() {
        bots.each {
            it.connect()
            sleep(200) // In case people abuse, it at least isn't instant.
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
        bots = []
    }

    def part(channel) {
        bots.each {
            it.part(channel)
        }
    }

    def slayAll() {
        bots.each {
            it.socket.close()
        }
        bots = []
    }
}
