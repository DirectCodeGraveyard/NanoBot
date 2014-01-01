package org.nanobot

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.nanobot.config.BotConfig

class NanoBot {
    def server
    def port
    def nickname
    HashMap<String, Channel> channels = [:]
    def debug = false
    private Socket socket
    def realName = 'NanoBot'
    def commandPrefix = '!'
    private IRCHandler ircHandler
    final HashMap<String, ArrayList<Closure>> handlers = [:]
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

    /**
     * Connects to the IRC Server
     * @return
     */
    void connect() {
    	if (socket != null && socket.connected)
    		throw new RuntimeException("Bot is already connected!")
        socket = new Socket()
        socket.connect(new InetSocketAddress(server as String, port as int))
        ircHandler = new IRCHandler(this, socket.inputStream.newReader(), new PrintStream(socket.outputStream))
    }

    @CompileStatic
    void dispatch(data, useThread) {
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
    void dispatch(data) {
        dispatch(data, true)
    }

    void on(String name, Closure closure) {
        if (handlers.containsKey(name)) {
            handlers.get(name).add(closure)
        } else {
            def newList = []
            newList.add(closure)
            handlers.put(name, newList)
        }
    }

    void join(channel) {
        send("JOIN $channel")
    }

    void part(channel) {
        send("PART $channel")
        dispatch(name: 'bot-part', channel: channel)
    }

    void msg(target, msg) {
        msg.toString().readLines().each {
            send("PRIVMSG $target :$it")
            sleep(500)
            dispatch(name: 'bot-message', target: target, message: it)
        }
    }

    void send(line) {
        ircHandler.send(line)
    }

    void disconnect(message) {
        send("QUIT :$message")
        while (!socket.closed);
    }

    void disconnect() {
        disconnect('Bot Disconnecting')
    }

    void enableCommandEvent() {
        on('message') {
            def user = it['user']
            def channel = it['channel']
            def msg = (it['message'] as String).trim()
            if (!msg.startsWith(commandPrefix)) {
                return
            }
            msg = msg.substring(commandPrefix.length())
            def split = msg.split()
            def args = split.drop(1)
            def cmd = split[0]
            dispatch(name: 'command', user: user, channel: channel, message: msg, split: split, args: args, command: cmd, false)
        }
    }

    void identify(password) {
        msg('NickServ', "identify $password")
    }

    void identify(user, password) {
        msg('NickServ', "identify $user $password")
    }

    void password(password) {
    	send("PASS ${password}")
    }

    void kick(channel, user) {
        send("KICK $channel $user")
    }

    void ban(channel, user) {
        mode(channel, user, '+b')
    }

    void kickBan(channel, user) {
        ban(channel, user)
        kick(channel, user)
    }

    void op(channel, user) {
        mode(channel, user, '+o')
    }

    void voice(channel, user) {
        mode(channel, user, '+v')
    }

    void mode(channel, user, mode) {
        send("MODE $channel $mode $user")
    }

    void useShutdownHook() {
        addShutdownHook {
            disconnect('Bot Stopped')
        }
    }

    void changeNick(newNick) {
        send("NICK $newNick")
        nickname = newNick
    }

    void notice(target, String msg) {
        msg.readLines().each {
            send("NOTICE $target :$it")
            dispatch(name: 'bot-notice', target: target, message: it)
        }
    }

    @Memoized
    static def parseNickname(String hostmask) {
        if (hostmask.startsWith(':')) {
            hostmask = hostmask.substring(1)
        }
        return hostmask.substring(0, hostmask.indexOf('!'))
    }

    void act(channel, message) {
        msg(channel, "\u0001ACTION ${message}\u0001")
    }

    void deop(channel, user) {
        mode(channel, user, '-o')
    }

    void devoice(channel, user) {
        mode(channel, user, '-v')
    }

    void unban(channel, user) {
        mode(channel, user, '-b')
    }

    void kick(channel, user, reason) {
        send("KICK $channel $user :$reason")
    }

    void kickBan(channel, user, reason) {
        ban(channel, user)
        kick(channel, user, reason)
    }

    boolean asBoolean() {
    	return socket != null && socket.connected
    }

    void call() {
    	if (!this) {
    		connect()
    	}
    }

    void call(args) {
    	args.each {
    		send(it as String)
    	}
    }
}
