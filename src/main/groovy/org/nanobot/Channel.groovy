package org.nanobot

class Channel {
    /**
     * Bot Associated with this Channel
     */
    NanoBot bot

    /**
     * Channel Topic
     */
    String topic

    /**
     * Channel Name
     */
    String name

    /**
     * Channel Users
     */
    List<String> users = []

    /**
     * Channel Operators
     */
    List<String> ops = []

    /**
     * Channel Voices
     */
    List<String> voices = []

    @Override
    String toString() {
        return name
    }

    /**
     * Ban a User
     * @param target user
     */
    def ban(target) {
        bot.ban(name, target)
    }

    /**
     * Unban a User
     * @param target user
     */
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

    def kick(target, reason) {
        bot.kick(name, target, reason)
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