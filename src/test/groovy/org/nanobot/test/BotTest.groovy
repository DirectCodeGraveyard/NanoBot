package org.nanobot.test

import org.junit.Test
import org.nanobot.NanoBot

import static org.junit.Assert.fail

class BotTest {
    private bot = new NanoBot("irc.esper.net", 6667, "JUnitTestBot")

    void setup() {
        bot.on('nick-in-use') {
            def origNick = it.original
            def newNick = "${origNick}_"
            println "The nickname ${origNick} is in use. Using ${newNick}."
            changeNick(newNick)
        }

        bot.on("ready") {
            try {
                join("#KenBot")
                msg("#KenBot", "Message Test")
                sleep(1000)
                notice("#KenBot", "Notice Test")
                sleep(1000)
                act("#KenBot", "is running tests")
                sleep(1000)
                part("#KenBot")
                sleep(1000)
                join("#KenBot")
                sleep(4000)
                disconnect("Test Complete")
            } catch(ignored) {
                disconnect("Tests Failed")
                fail("Bot Tests Failed")
            }
        }

        bot.on("command") { e ->
            msg(e.channel, "Command: ${e.command}")
        }
    }

    @Test
    void quickTest() {
        setup()
        bot.connect()
        while(!bot.states.has("quit"));
    }
}