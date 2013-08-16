package org.nanobot

import groovy.transform.CompileStatic

class ConnectionHandler implements Runnable {
    def BufferedReader reader
    def PrintStream writer
    def NanoBot bot
    def Thread thread
    def ready = false

    ConnectionHandler(NanoBot bot, BufferedReader reader, PrintStream writer) {
        this.bot = bot
        this.reader = reader
        this.writer = writer
        this.thread = new Thread(this)
        thread.setName('NanoBot-InputHandler')
        thread.start()
        bot.dispatch(name: 'connect')
        writer.println 'NICK ' + bot.nickname
        writer.println "USER ${bot.userName} * 8 :${bot.realName}"
        bot.dispatch(name: 'post-connect')
    }

    @Override
    @CompileStatic
    void run() {
        def line
        while ((line = reader.readLine()) != null) {
            def split = line.split(' ')
            if (bot.debug) println line
            if (split[0].equals('PING')) {
                bot.dispatch(name: 'ping', id: split[1].substring(1))
                writer.println 'PONG ' + split[1]
                if (!ready) {
                    ready = true
                    bot.dispatch(name: 'ready')
                }
            } else if (split[1].equals('PRIVMSG')) {
                def from = split[0].substring(1, split[0].indexOf('!'))
                def msg = split.drop(0).drop(1).drop(2).join(' ').substring(1)
                bot.dispatch(name: 'message', to: split[2], from: from, message: msg)
            } else if (split[1]=='332') { // Topic is being sent
                def topic = split.drop(4).join(' ').substring(1)
                bot.getTopics()[split[3]] = topic
                bot.dispatch(name: 'topic', channel: split[3], topic: topic)
            } else if (split[0]=='ERROR') {
                println 'Failed to connect: ' + split.drop(1).join(' ').substring(1)
            }
        }
    }
}
