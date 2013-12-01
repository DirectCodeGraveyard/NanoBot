package org.nanobot.bot

import org.nanobot.NanoBot
import org.nanobot.config.BotConfig

class MainIRCBot {
    static NanoBot bot

    static void main(String[] args) {
        bot = new NanoBot(new BotConfig(new File("bot.cfg")))
        connect()
    }

    static def connect() {
        bot.connect()
    }

    static def disconnect() {
        bot.disconnect()
    }

    static void setup() {
        bot.enableCommandEvent()

        bot.on('connect') {
            println "Connected."
        }

        bot.on('nick-in-use') {
            def origNick = it.original
            def newNick = "${origNick}_"
            println "The nickname ${origNick} is in use. Using ${newNick}."
            bot.changeNick(newNick)
        }

        bot.on('bot-join') {
            println "Joined ${it.channel}"
        }

        bot.on('topic') {
            println "Topic for ${it.channel}: ${it.topic}"
        }

        bot.on('message') {
            println "<${it.channel}><${it.user}> ${it.message}"
        }

        bot.on('pm') {
            println "<${it.user}> ${it.message}"
        }

        bot.on('bot-part') {
            println "Left ${it.channel}"
        }
    }
}
