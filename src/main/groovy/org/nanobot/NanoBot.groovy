package org.nanobot

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.nanobot.config.BotConfig

/**
 * An IRC Bot Written for Simplicity but usefulness
 */
class NanoBot {
    def server
    def port
    def nickname
    final HashMap<String, Channel> channels = [:]
    private Socket socket
    def realName = 'NanoBot'
    def commandPrefix = '!'
    private IRCHandler ircHandler
    final Map<String, List<Closure>> handlers = [:].withDefault { [] }
    def userName = 'NanoBot'
    def debug = false
    final StateContainer states = new StateContainer()
    final EventProxy events = new EventProxy(this)

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
     */
    void connect() {
    	if (socket != null && socket.connected)
    		throw new RuntimeException("Bot is already connected!")
        channels.clear()
        states.reset()
        socket = new Socket()
        socket.connect(new InetSocketAddress(server as String, port as int))
        ircHandler = new IRCHandler(this, socket.inputStream.newReader(), new PrintStream(socket.outputStream))
    }

    /**
     * Dispatches an Event
     * @param data Event Parameters
     * @param useThread whether to use a thread
     */
    @CompileStatic
    void dispatch(data, useThread) {
        def name = data['name'] as String
        if (name == null || !handlers.containsKey(name))
            return
        def handlers = handlers[name] as List<Closure>
        def execute = { ->
            handlers.each { Closure handler ->
                handler.call(data)
            }
        }
        if (useThread) {
            def thread = Thread.startDaemon("BotEvent[${name}]", execute)
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace()
                }
            })
        } else {
            execute()
        }
    }

    /**
     * Dispatches an Event
     * @param data Event Parameters
     */
    @CompileStatic
    void dispatch(data) {
        dispatch(data, true)
    }

    /**
     * Registers an Event Handler
     * @param name Event Name
     * @param handler Event Handler
     * @see EventProxy
     */
    void on(String name, Closure handler) {
        handler.delegate = this
        handlers[name] += handler
    }

    /**
     * Joins a Channel
     * @param channel Channel to Join
     */
    void join(channel) {
        send("JOIN $channel")
    }

    /**
     * Leaves a Channel
     * @param channel Channel to Leave
     */
    void part(channel) {
        send("PART $channel")
        dispatch(name: 'bot-part', channel: channel)
    }

    /**
     * Messages a Target
     * @param target Target
     * @param msg Message
     */
    void msg(target, msg) {
        msg.toString().readLines().each {
            send("PRIVMSG $target :$it")
            sleep(100)
            dispatch(name: 'bot-message', target: target, message: it)
        }
    }

    /**
     * Send a Raw Line through IRC
     * @param line
     */
    void send(line) {
        ircHandler.send(line)
    }

    /**
     * Quit the Server
     * @param message Message to Send
     */
    void disconnect(message) {
        send("QUIT :$message")
        while (!socket.closed);
    }

    /**
     * Quit the Server with message 'Bot Disconnecting'
     */
    void disconnect() {
        disconnect('Bot Disconnecting')
    }

    /**
     * Setup Command Event to be usable
     */
    void enableCommandEvent() {
        on('message') {
            def user = it['user']
            def channel = it['channel']
            def msg = (it['message'] as String).trim()
            if (!msg.startsWith(commandPrefix))
                return
            msg = msg.substring(commandPrefix.length())
            def split = msg.split()
            def args = split.drop(1)
            def cmd = split[0]
            dispatch(name: 'command', user: user, channel: channel, message: msg, split: split, args: args, command: cmd, false)
        }
    }

    /**
     * Identifies with NickServ
     * @param password Password
     */
    void identify(password) {
        msg('NickServ', "identify $password")
    }

    /**
     * Identifies with NickServ
     * @param user Username
     * @param password Password
     */
    void identify(user, password) {
        msg('NickServ', "identify $user $password")
    }

    /**
     * Sends the Server Password
     * @param password Server Password
     */
    void password(password) {
    	send("PASS ${password}")
    }

    /**
     * Kicks a User from a Channel
     * @param channel Channel
     * @param user User
     */
    void kick(channel, user) {
        send("KICK $channel $user")
    }

    /**
     * Bans a User from a Channel
     * @param channel Channel
     * @param user User
     */
    void ban(channel, user) {
        mode(channel, user, '+b')
    }

    /**
     * Kicks then bans a User from a Channel
     * @param channel Channel
     * @param user user
     */
    void kickBan(channel, user) {
        ban(channel, user)
        kick(channel, user)
    }

    /**
     * Ops a User in a Channel
     * @param channel Channel
     * @param user User
     */
    void op(channel, user) {
        mode(channel, user, '+o')
    }

    /**
     * Voices a User in a Channel
     * @param channel Channel
     * @param user User
     */
    void voice(channel, user) {
        mode(channel, user, '+v')
    }

    /**
     * Gives the User in the Channel a Mode
     * @param channel Channel
     * @param user User
     * @param mode Mode
     */
    void mode(channel, user, mode) {
        send("MODE $channel $mode $user")
    }

    /**
     * Instructs NanoBot to register a shutdown hook to Quit the Server
     */
    void useShutdownHook() {
        addShutdownHook {
            disconnect('Bot Stopped')
        }
    }

    /**
     * Changes the Bots Nickname
     * @param newNick new nickname
     */
    void changeNick(newNick) {
        send("NICK $newNick")
        nickname = newNick
    }

    /**
     * Sends a Notice to the Target
     * @param target Target
     * @param msg Message
     */
    void notice(target, msg) {
        msg.toString().readLines().each {
            send("NOTICE $target :$it")
            dispatch(name: 'bot-notice', target: target, message: it)
        }
    }

    /**
     * Parses a Hostmask to retrieve the Nickname
     * @param hostmask Hostmask to parse
     * @return nickname
     */
    @Memoized(maxCacheSize = 20)
    protected static def parseNickname(String hostmask) {
        return parseHostmask(hostmask)["nickname"]
    }

    /**
     * Parses a Hostmask into different parts
     * @param hostmask
     * @return map of nickname, and host
     */
    @Memoized(maxCacheSize = 20)
    protected static def parseHostmask(String hostmask) {
        if (hostmask.startsWith(':'))
            hostmask = hostmask.substring(1)
        def nickname = hostmask[0..hostmask.indexOf("!") - 1]
        def host = hostmask[hostmask.indexOf("@")..hostmask.size() - 1]
        return [
                nickname: nickname,
                host: host
        ]
    }

    /**
     * Sends an Action in a Channel
     * @param channel Channel
     * @param message Message
     */
    void act(channel, message) {
        msg(channel, "\u0001ACTION ${message}\u0001")
    }

    /**
     * Deops a User in a Channel
     * @param channel Channel
     * @param user User
     */
    void deop(channel, user) {
        mode(channel, user, '-o')
    }

    /**
     * Devoices a User in a Channel
     * @param channel Channel
     * @param user User
     */
    void devoice(channel, user) {
        mode(channel, user, '-v')
    }

    /**
     * Unbans a User from a Channel
     * @param channel Channel
     * @param user User
     */
    void unban(channel, user) {
        mode(channel, user, '-b')
    }

    /**
     * Kicks a User from a Channel with a Reason
     * @param channel Channel
     * @param user User
     * @param reason Reason
     */
    void kick(channel, user, reason) {
        send("KICK $channel $user :$reason")
    }

    /**
     * Kicks then bans a User from a Channel with a Reason
     * @param channel Channel
     * @param user User
     * @param reason Reason
     */
    void kickBan(channel, user, reason) {
        ban(channel, user)
        kick(channel, user, reason)
    }

    /**
     * Gives Groovy Truth to the Bot
     * @return true if bot is connected
     */
    boolean asBoolean() {
    	return socket != null && socket.connected
    }

    /**
     * Connects to the Server if not already connected
     */
    void call() {
    	if (!this)
    		connect()
    }

    /**
     * Sends the lines specified as arguments to the server
     * @param args Lines to send to the server
     */
    void call(args) {
    	args.each {
    		send(it as String)
    	}
    }
}
