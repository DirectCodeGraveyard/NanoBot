package org.nanobot.bot

import groovy.io.FileType
import org.codehaus.groovy.control.CompilerConfiguration
import org.nanobot.BotDSL
import org.nanobot.BotDSLScript
import org.nanobot.NanoBot
import org.nanobot.Utils
import org.nanobot.config.GConfig

class MainIRCBot {
    static NanoBot bot
    static Map<String, Closure> commands = [:]
    static config = new GConfig(new File("config.groovy"))
    static scriptDir = new File("scripts")
    static List<String> admins

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

        admins = botConfig["admins"] as List<String>

        bot.server = serverConfig["host"] ?: "irc.esper.net"
        bot.port = serverConfig["port"] ?: 6667

        bot.nickname = botConfig["nickname"] ?: "SuperNanoBot"
        bot.userName = botConfig["username"] ?: "SuperNanoBot"
        bot.realName = botConfig["realname"] ?: "NanoBot"
        bot.commandPrefix = botConfig["commandPrefix"] ?: "!"

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
            changeNick(newNick)
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

        bot.on('ctcp') {
            if (it.type == "ACTION") {
                println "<${it.channel}> * ${it.user} ${it.message}"
            } else {
                println "<${it.channel}> Received '${it.type}' from '${it.user}' containing '${it.message}'"
            }
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
                    msg(event.channel, it)
                }
                def handler = commands[command]
                handler.delegate = bot
                handler(event)
            }
        }

        commands["hi"] = {
            it.reply("> Hello!")
        }

        def binding = [
                bot: bot,
                commands: commands,
                fetch: { String url ->
                    url.toURL().getText()
                },
                parseJSON: { String content ->
                    Utils.parseJSON(content)
                },
                parseXML: { String content ->
                    Utils.parseXML(content)
                },
                encodeJSON: { Object obj, boolean pretty = false ->
                    Utils.encodeJSON(obj, pretty)
                },
                file: { File parent = null, String path ->
                    new File(parent, path)
                },
                admins: admins,
                dsl: new BotDSL(bot) {

                    @Override
                    Object run() {
                        return null
                    }
                }
        ] as Binding

        commands["r"] = {
            def user = it.user as String
            if (!admins.contains(user))
                it.reply("> Sorry, only admins may use this command.")
            else {
                try {
                    binding.setVariable("user", user)
                    binding.setVariable("channel", it.channel)
                    binding.setVariable("say", it.reply)
                    Utils.runScript(it.args.join(" ") as String, binding)
                } catch (e) {
                    bot.notice(it.user, "Exception Thrown: ${e.class.name}: ${e.message}")
                }
            }
        }

        commands["dsl"] = {
            def user = it.user as String
            if (!admins.contains(user))
                it.reply("> Sorry, only admins may use this command.")
            else {
                def config = new CompilerConfiguration()
                config.scriptBaseClass = BotDSLScript.class.name
                def shell = new GroovyShell(config)
                shell.setVariable("dsl", new BotDSL(bot ))
                def script = shell.parse(it.args.join(" ") as String) as BotDSLScript
                script.run()
            }
        }
    }

    static def loadScripts() {
        scriptDir.eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith(".groovy")) {
                Utils.runScript(it.text, [
                        bot: bot,
                        commands: commands,
                        admins: admins
                ])
            }
        }
    }
}
