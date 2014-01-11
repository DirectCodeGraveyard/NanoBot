package org.nanobot.bot

import groovy.io.FileType
import org.nanobot.NanoBot
import org.nanobot.Utils
import org.nanobot.config.GConfig

class MainIRCBot {
    static NanoBot bot
    static Map<String, Closure> commands = [:]
    static config = new GConfig(new File("bot.cfg"))
    static scriptDir = new File("scripts")

    static void main(String[] args) {
        bot = new NanoBot()
        setup()
        connect()
    }

    static def connect() {
        bot.connect()
    }

    static def disconnect() {
        bot.disconnect()
    }

    static void setup() {

        if (!scriptDir.exists())
            scriptDir.mkdirs()

        // This is kind of messy, but we can live with it.
        config.setDefaultConfig("server = [\n        host: \"irc.esper.net\",\n        port: 6667\n]\n\nbot = [\n        nickname: \"SuperNanoBot\",\n        username: \"SuperNanoBot\",\n        realname: \"NanoBot\",\n        channels: [\n            \"#DirectMyFile\"\n        ],\n        admins: []\n]\n")

        config.load()

        bot.enableCommandEvent()

        def serverConfig = config.getProperty("server", [
                host: "irc.esper.net",
                port: 6667
        ])

        def botConfig = config.getProperty("bot", [
                nickname: "SuperNanoBot",
                username: "SuperNanoBot",
                realname: "NanoBot",
                channels: [
                        "DirectMyFile"
                ],
                commandPrefix: "!",
                admins: []
        ])

        bot.server = serverConfig["host"]
        bot.port = serverConfig["port"]

        bot.nickname = botConfig["nickname"]
        bot.userName = botConfig["username"]
        bot.realName = botConfig["realname"]
        bot.commandPrefix = botConfig["commandPrefix"]

        loadScripts()

        bot.on('connect') {
            println "Connected."
        }

        bot.on('ready') {
            sleep(1000)
            botConfig["channels"].each {
                bot.join(it)
            }
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

        bot.on('command') { Map<String, Object> event ->
            def command = event.command as String

            if (commands.containsKey(command)) {
                event.reply = {
                    bot.msg(event.channel, it)
                }
                commands[command](event)
            }
        }

        commands["hi"] = {
            it.reply("> Hello!")
        }

        commands["r"] = {
            def user = it.user as String
            def admins = botConfig["admins"] as List<String>
            if (!admins.contains(user))
                it.reply("> Sorry, only admins may use this command.")
            else {
                try {
                    Utils.runScript(it.args.join(" ") as String, [
                            bot: bot,
                            commands: commands
                    ])
                } catch (e) {
                    bot.notice(it.user, "Exception Thrown: ${e.class.name}: ${e.message}")
                }
            }
        }
    }

    static def loadScripts() {
        scriptDir.eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith(".groovy")) {
                Utils.runScript(it.text, [
                        bot: bot,
                        commands: commands
                ])
            }
        }
    }
}
