package org.nanobot

class Channel {
    def NanoBot bot
    def String topic
    def String name
    def ArrayList<String> users = []
    def ArrayList<String> ops = []
    def ArrayList<String> voices = []

    @Override
    String toString() {
        return name
    }

    def ban(target) {
        bot.ban(name, target)
    }

    def unban(target) {
        bot.unban(name, target)
    }

    def msg(message) {
        bot.msg(name, message)
    }

    def msg(user, message) {
        bot.msg(user, message)
    }

    def op(target) {
        bot.op(name, target)
    }

    def deop(target) {
        bot.deop(name, target)
    }

    def voice(target) {
        bot.voice(name, target)
    }

    def devoice(target) {
        bot.devoice(name, target)
    }

    def kick(target) {
        bot.kick(name, target)
    }

    def kickBan(target) {
        bot.kickBan(name, target)
    }

    def kickBan(target, reason) {
        bot.kickBan(target, reason)
    }

    def mode(user, mode) {
        bot.mode(name, user, mode)
    }

    def act(message) {
        bot.act(name, message)
    }

    def notice(message) {
        bot.notice(name, message)
    }

    def notice(user, message) {
        bot.notice(user, message)
    }
}
