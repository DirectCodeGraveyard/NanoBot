package org.nanobot

import groovy.transform.CompileStatic
import org.nanobot.config.BotConfig

class NanoBot {
    def server
    def port
    def nickname
    HashMap<String, String> topics = [:]
    def debug = false
    def socket = new Socket()
    def realName = 'NanoBot'
    def commandPrefix = '!'
    IRCHandler ircHandler
    HashMap<String, ArrayList<Closure>> handlers = [:]
    def userName = 'NanoBot'

    NanoBot() {}

    NanoBot(server, port, nickname) {
        this()
        this.server = server
        this.port = port
        this.nickname = nickname
    }

    NanoBot(BotConfig botConfig) {
        this()
        server = botConfig.getServer().get('host') as String
        port = botConfig.getServer().get('port', 6667) as int
        nickname = botConfig.getBot().get('nickname', 'NanoBot')
        def channels = botConfig.getBot().get('channels', [])
        on('ready') {
            channels.each {
                join(it)
            }
        }
        botConfig.save()
    }

    def connect() {
        socket.connect(new InetSocketAddress(server as String, port as int))
        ircHandler = new IRCHandler(this, socket.inputStream.newReader(), new PrintStream(socket.outputStream))
    }

    @CompileStatic
    def dispatch(data, useThread) {
        def name = data['name']
        if (name==null || !handlers.containsKey(name)) {return}
        def handlers = handlers.get(name)
        handlers.each { Closure it ->
            it.delegate = this
            if (useThread) {
                Thread.startDaemon { ->
                    it.call(data)
                }
            } else {
                it.call(data)
            }
        }
    }

    @CompileStatic
    def dispatch(data) {
        dispatch(data, true)
    }

    def on(String name, Closure closure) {
        if (handlers.containsKey(name)) {
            handlers.get(name).add(closure)
        } else {
            def newList = []
            newList.add(closure)
            handlers.put(name, newList)
        }
    }

    def getHandlers() {
        return handlers
    }

    def join(channel) {
        send("JOIN $channel")
        dispatch(name: 'bot-join', channel: channel)
    }

    def part(channel) {
        send("PART $channel")
        dispatch(name: 'bot-part', channel: channel)
    }

    def msg(target, String msg) {
        msg.readLines().each {
            send("PRIVMSG $target :$it")
            sleep(500)
            dispatch(name: 'bot-message', target: target, message: it)
        }
    }

    def send(line) {
        ircHandler.send(line)
    }

    def disconnect(message) {
        send("QUIT :$message")
        while (!socket.closed);
    }

    def disconnect() {
        disconnect('Bot Disconnecting')
    }

    def enableCommandEvent() {
        on('message') {
            def user = it['user']
            def channel = it['channel']
            def msg = (it['message'] as String).trim()
            if (!msg.startsWith(commandPrefix)) {
                return
            }
            msg = msg.substring(commandPrefix.length())
            def split = msg.split(' ')
            def args = split.drop(1)
            def cmd = split[0]
            dispatch(name: 'command', user: user, channel: channel, message: msg, split: split, args: args, command: cmd, false)
        }
    }

    def identify(password) {
        msg('NickServ', "identify $password")
    }

    def identify(user, password) {
        msg('NickServ', "identify $user $password")
    }

    def kick(channel, user) {
        send("KICK $channel $user")
    }

    def ban(channel, user) {
        mode(channel, user, '+b')
    }

    def kickBan(channel, user) {
        ban(channel, user)
        kick(channel, user)
    }

    def op(channel, user) {
        mode(channel, user, '+o')
    }

    def voice(channel, user) {
        mode(channel, user, '+v')
    }

    def mode(channel, user, mode) {
        send("MODE $channel $mode $user")
    }

    def useShutdownHook() {
        addShutdownHook {
            disconnect('Bot Stopped')
        }
    }

    def changeNick(newNick) {
        send("NICK $newNick")
    }

    def notice(target, String msg) {
        msg.split('\n').each {
            send("NOTICE $target :$it")
            dispatch(name: 'bot-notice', target: target, message: it)
        }
    }

    private static parseNickname = { String hostmask ->
        if (hostmask.startsWith(':')) {
            hostmask = hostmask.substring(1)
        }
        return hostmask.substring(0, hostmask.indexOf('!'))
    }.memoize()

    static def parseNickname(String hostmask) {
        return parseNickname.call(hostmask)
    }

    def act(channel, message) {
        msg(channel, "\u0001ACTION ${message}\u0001")
    }

    def deop(channel, user) {
        mode(channel, user, '-o')
    }

    def devoice(channel, user) {
        mode(channel, user, '-v')
    }

    def unban(channel, user) {
        mode(channel, user, '-b')
    }

    def kick(channel, user, reason) {
        send("KICK $channel $user :$reason")
    }

    def kickBan(channel, user, reason) {
        ban(channel, user)
        kick(channel, user, reason)
    }
}
