package org.nanobot

import groovy.transform.CompileStatic

@SuppressWarnings("GroovyIfStatementWithTooManyBranches")
class IRCHandler implements Runnable {
    def BufferedReader reader
    def PrintStream writer
    def NanoBot bot
    def Thread thread
    Thread timeoutThread

    IRCHandler(NanoBot bot, BufferedReader reader, PrintStream writer) {
        this.bot = bot
        this.reader = reader
        this.writer = writer
        this.thread = Thread.start("NanoBot-InputHandler", {
            this.run()
        })
        this.timeoutThread = Thread.startDaemon { ->
            while (bot.socket.isConnected()) {
                if (bot.lastPingTime != -1 && (System.currentTimeMillis() - bot.lastPingTime) >= 120000)
                    bot.dispatch(name: "timeout", lastPing: bot.lastPingTime)
                sleep(2000)
            }
        }
        bot.states.on("connect")
        bot.dispatch(name: 'connect')
        send("NICK ${bot.nickname}")
        send("USER ${bot.userName} * 8 :${bot.realName}")
        bot.dispatch(name: 'post-connect')
        bot.states.toggle("connect")
        bot.states.on("connected")
    }

    @Override
    @CompileStatic
    void run() {
        reader.eachLine { String line ->
            def split = line.tokenize()
            if (bot.debug)
                println line
            if (split[0] == 'PING') {
                bot.lastPingTime = System.currentTimeMillis()
                bot.dispatch(name: 'ping', id: split[1].substring(1))
                writer.println "PONG ${split[1]}"
                if (!bot.states.has("ready")) {
                    bot.states.on("ready")
                    bot.dispatch(name: 'ready')
                }
            } else if (split[1] == 'PRIVMSG' && split[2].startsWith('#')) { // Channel Message
                def sender = NanoBot.parseNickname(split[0])
                def msg = split.drop(3).join(' ').substring(1)
                if (msg.startsWith('\u0001')) {
                    def type = msg.substring(1, msg.contains(" ") ? msg.indexOf(" ") : msg.size() - 1)
                    msg = msg.substring(type.size() + 2).replaceAll('\u0001', '')
                    bot.dispatch(name: "ctcp", user: sender, channel: bot.channels[split[2]], type: type, message: msg)
                } else {
                    bot.dispatch(name: 'message', channel: bot.channels[split[2]], user: sender, message: Colors.removeFormattingAndColors(msg), rawMessage: msg)
                }
            } else if (split[1] == 'PRIVMSG' && !(split[2].startsWith('#'))) { // Private Message
                def user = NanoBot.parseNickname(split[0])
                def msg = split.drop(3).join(' ').substring(1)
                if (msg.startsWith('\u0001')) {
                    msg = msg.substring(1)
                    def type = msg.substring(1, msg.indexOf(" ") ?: msg.size() - 1)
                    bot.dispatch(name: "priv-ctcp", user: user, type: type, message: msg.substring(type.size()))
                }
                bot.dispatch(name: 'pm', user: user, message: Colors.removeFormattingAndColors(msg), rawMessage: msg)
            } else if (split[1] == '332') { // Topic is being sent on join
                def topic = split.drop(4).join(' ').substring(1)
                bot.channels[split[3]]?.topic = topic
                bot.dispatch(name: 'topic', channel: split[3], topic: topic)
            } else if (split[0] == 'ERROR') { // Error has occurred
                bot.dispatch(name: 'error', message: split.drop(1).join(' ').substring(1))
            } else if (split[1] == 'TOPIC') { // Topic was changed
                def user = NanoBot.parseNickname(split[0])
                def channel = split[2]
                def topic = split.drop(3).join(' ').substring(1)
                bot.channels[channel].topic = topic
                bot.dispatch(name: 'topic', channel: bot.channels[channel], topic: topic, user: user)
            } else if (split[1] == 'INVITE') { // Invited to Channel
                def user = NanoBot.parseNickname(split[0])
                def channel = split[3].substring(1)

                bot.dispatch(name: 'invite', user: user, channel: channel)
            } else if (split[1] == 'NICK') { // Someones nick was changed
                def original = NanoBot.parseNickname(split[0])
                def newNick = split[2].substring(1)

                bot.dispatch(name: 'nick-change', new: newNick, original: original)
            } else if (split[1] == '433') { // Nickname is in Use
                bot.dispatch(name: 'nick-in-use', original: split[3])
            } else if (split[1] == 'KICK') {
                if (split[3] == bot.nickname) {
                    bot.dispatch(name: 'bot-kick' , channel: split[2], user: NanoBot.parseNickname(split[0]))
                }
            } else if (split[1] == '353') { // Received User List
                def names = split.drop(5)
                def channel = bot.channels[split[4]]
                names.each { String name ->
                    if (name.startsWith(':')) {
                        name = name.substring(1)
                    }
                    if (name.startsWith('@')) {
                        channel.users.add(name.substring(1))
                        channel.ops.add(name.substring(1))
                    } else if (name.startsWith('+')) {
                        channel.users.add(name.substring(1))
                        channel.voices.add(name.substring(1))
                    } else {
                        channel?.users?.add(name)
                    }
                }
            } else if (split[1] == 'JOIN') { // Somebody joined the Channel
                def user = NanoBot.parseNickname(split[0])
                def chan = split[2]
                if (chan.startsWith(":"))
                    chan = chan.substring(1)
                if (user == bot.nickname) {
                    def channel = new Channel()
                    channel.name = chan
                    channel.bot = bot
                    bot.channels[channel.name] = channel
                    bot.dispatch(name: 'bot-join', channel: channel)
                    return
                }
                bot.dispatch(name: 'join', user: user, channel: bot.channels[split[2]])
                bot.channels[split[2]]?.users += (user as String)
            } else if (split[1] == 'PART') {
                def user = NanoBot.parseNickname(split[0])
                bot.dispatch(name: 'part', user: user, channel: split[2])
                removeUser(split[2], user)
            } else if (split[1] == 'MODE') {
                def m = split[3]
                def channel = bot.channels[split[2]]
                if (split.size() >= 5) {
                    def target = split[4]
                    if (m == '+v') {
                        channel.voices += target
                    } else if (m == '+o') {
                        channel.ops += target
                    } else if (m == '-o') {
                        channel.ops.remove(target)
                    } else if (m == '-v') {
                        channel.voices.remove(target)
                    }
                }
                bot.dispatch(name: "mode", mode: m, channel: channel)
            } else if (split[1] == 'QUIT') { // Somebody has quit the server
                def user = NanoBot.parseNickname(split[0])
                def reason = split.drop(2).join(" ").substring(1)
                bot.channels.keySet().each { String it ->
                    removeUser(it, user)
                }
                bot.dispatch(name: 'quit', user: user, reason: reason)
            }
        }
        bot.states.on("quit")
    }

    def send(line) {
        writer.println(line)
    }

    def removeUser(String channel, user) {
        def c = bot.channels[channel]
        c?.users?.remove(user)
        c?.ops?.remove(user)
        c?.voices?.remove(user)
    }
}