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
    ConnectionHandler connection
    HashMap<String, ArrayList<Closure>> handlers = [:]
    def userName = 'NanoBot'

    NanoBot() {
        addShutdownHook {
            disconnect()
        }
    }

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
        connection = new ConnectionHandler(this, socket.inputStream.newReader(), new PrintStream(socket.outputStream))
    }

    @CompileStatic
    def dispatch(data) {
        def name = data['name']
        if (name==null || !handlers.containsKey(name)) {return}
        def handlers = handlers.get(name)
        handlers.each { Closure it ->
            Thread.startDaemon { ->
                it.call(data)
            }
        }
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

    def join(channel) {
        send("JOIN $channel")
        dispatch(name: 'bot-join', channel: channel)
    }

    def part(channel) {
        send("PART $channel")
        dispatch(name: 'bot-part', channel: channel)
    }

    def msg(target, String msg) {
        if (target.is(null)) {
            target = 'kaendfinger'
        }
        msg.split('\n').each {
            send("PRIVMSG $target :$it")
            dispatch(name: 'bot-message', target: target, message: it)
        }
    }

    def send(line) {
        connection.send(line)
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
            def channel = it['to']
            def msg = (it['message'] as String).trim()
            if (!msg.startsWith('!')) {
                return
            }
            def split = msg.split(' ')
            def cmd = split[0].trim().substring(1)
            dispatch(name: 'command', user: user, channel: channel, message: msg, split: split, command: cmd)
        }
    }

    static def getNick(String hostmask) {
        return hostmask.substring(0, hostmask.indexOf('!'))
    }
}
