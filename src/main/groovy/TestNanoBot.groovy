import org.nanobot.NanoBot

def bot = new NanoBot()

bot.server = 'irc.esper.net'
bot.port = 6667
bot.nickname = 'SuperNanoBot'
bot.realName = 'NanoBot by kaendfinger'

bot.enableCommandEvent()

bot.on('ready') {
    bot.join('#Minetweak')
}

bot.on('connect') {
    println 'Connected.'
}

bot.on('command') {
    def cmd = it['command']
    if (cmd=='hi') {
        bot.msg(it['channel'], 'Hi')
    } else {
        bot.msg(it['channel'], "> $cmd is not a known command.")
    }
}

bot.on('topic') {
    println "Topic for ${it['channel']}: ${it['topic']}"
}

bot.on('bot-join') {
    println "Joined ${it['channel']}"
}

bot.on('bot-part') {
    println "Left ${it['channel']}"
}

bot.connect()