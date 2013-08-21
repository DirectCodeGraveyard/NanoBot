package org.nanobot

import groovy.transform.CompileStatic

class IRCHandler implements Runnable {
    def BufferedReader reader
    def PrintStream writer
    def NanoBot bot
    def Thread thread
    def ready = false

    IRCHandler(NanoBot bot, BufferedReader reader, PrintStream writer) {
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
        reader.eachLine { String line ->
            def split = line.split(' ')

            if (split[0]=='PING') {
                bot.dispatch(name: 'ping', id: split[1].substring(1))
                writer.println 'PONG ' + split[1]
                if (!ready) {
                    ready = true
                    bot.dispatch(name: 'ready')
                }
            } else if (split[1]=='PRIVMSG' && split[2].startsWith('#')) { // Channel Message
                def sender = NanoBot.parseNickname(split[0])
                def msg = split.drop(3).join(' ').substring(1)
                bot.dispatch(name: 'message', channel: split[2], user: sender, message: msg)
            } else if (split[1]=='PRIVMSG' && !(split[2].startsWith('#'))) { // Private Message
                def user = NanoBot.parseNickname(split[0])
                def msg = split.drop(3).join(' ').substring(1)
                bot.dispatch(name: 'pm', user: user, message: msg)
            } else if (split[1]=='332') { // Topic is being sent on join
                def topic = split.drop(4).join(' ').substring(1)
                bot.getTopics()[split[3]] = topic
                bot.dispatch(name: 'topic', channel: split[3], topic: topic)
            } else if (split[0]=='ERROR') { // Error has occurred
                println split.drop(1).join(' ').substring(1)
            } else if (split[1]=='TOPIC') { // Topic was changed
                def user = NanoBot.parseNickname(split[0])
                def channel = split[2]
                def topic = split.drop(3).join(' ').substring(1)
                bot.getTopics()[channel] = topic
                bot.dispatch(name: 'topic', channel: channel, topic: topic, user: user)
            } else if (split[1]=='INVITE') { // Invited to Channel
                def user = NanoBot.parseNickname(split[0])
                def channel = NanoBot.parseNickname(split[3].substring(1))

                bot.dispatch(name: 'invite', user: user, channel: channel)
            } else if (split[1]=='NICK') { // Someones nick was changed
                def original = NanoBot.parseNickname(split[0])
                def newNick = split[2].substring(1)

                bot.dispatch(name: 'nick-change', new: newNick, original: original)
            } else if (split[1]=='433') { // Nickname is in Use
                bot.dispatch(name: 'nick-in-use', original: split[3])
            }
            if (bot.debug) println line
        }
    }

    def send(line) {
        writer.println line
    }
}
