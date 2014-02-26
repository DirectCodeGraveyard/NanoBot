package org.nanobot

class BotDSL {
    private NanoBot bot

    BotDSL(NanoBot bot) {
        this.bot = bot
    }

    void ban(Map<String, Object> options) {
        def user = options["user"] as String
        def channel = options["channel"] as String
        def kick = options["kick"] as boolean ?: false
        def reason = options["reason"] as String ?: "No Reason Given"
        bot.ban(channel, user)
        if (kick) {
            bot.kick(channel, user, reason)
        }
    }

    void kick(Map<String, Object> options) {
        def user = options["user"] as String
        def channel = options["channel"] as String
        def reason = options["reason"] as String ?: "No Reason Given"
        bot.kick(channel, user, reason)
    }

    void join(Map<String, Object> options) {
        def channel = options["channel"] as String
        bot.join(channel)
    }

    void part(Map<String, Object> options) {
        def channel = options["channel"] as String
        bot.part(channel)
    }

    void message(Map<String, Object> options) {
        def target = options["target"] ?: options["user"] ?: options["channel"]
        def message = options["message"]
        bot.msg(target, message)
    }

    void action(Map<String, Object> options) {
        def target = options["target"] ?: options["user"] ?: options["channel"]
        def message = options["message"]
        bot.act(target, message)
    }

    @SuppressWarnings("GroovyIfStatementWithTooManyBranches")
    void send(Map<String, Object> options) {
        def target = options["target"] ?: options["user"] ?: options["channel"]
        def message = options["message"]
        def line = options["line"]
        def action = options["action"] ?: options["me"]
        if (target && message) {
            bot.msg(target, message)
        } else if (line) {
            bot.send(line)
        } else if (target && action) {
            bot.act(target, action)
        } else {
            new UnsupportedOperationException("Invalid 'send' operation with parameters: ${options}")
        }
    }

    void call(@DelegatesTo(BotDSL) Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this
        closure()
    }
}
