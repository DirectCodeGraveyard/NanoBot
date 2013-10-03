import org.nanobot.NanoBot

def useTimer = false

def debug = true

def timer = { String name, Closure c ->
    if (!useTimer) {c() ; return}
    def startTime = System.currentTimeMillis()
    c()
    def endTime = System.currentTimeMillis()
    println "$name was executed in ${endTime - startTime}ms"
}

timer("Creating Bot") {
    bot = new NanoBot()
}

bot.server = 'irc.esper.net'
bot.port = 6667
bot.nickname = 'NanoBot'
bot.realName = 'NanoBot by kaendfinger'
bot.debug = debug

bot.enableCommandEvent()

bot.on('ready') {
    bot.join('#DirectMyFile')
}

bot.on('connect') {
    println 'Connected'
}

bot.on('post-connect') {
    println 'Sent User Information'
}

bot.on('command') {
    timer("Command") {
        def cmd = it.command
        if (cmd=='hi') {
            bot.msg(it.channel, 'Hi')
        } else {
            bot.msg(it.channel, "> $cmd is not a known command.")
        }
    }
}

bot.on('topic') {
    println "Topic for ${it.channel}: ${it.topic}"
}

bot.on('bot-join') {
    println "Joined ${it.channel}"
}

bot.on('bot-part') {
    println "Left ${it.channel}"
}

bot.on('message') {
    println "<${it.channel}><${it.user}> ${it.message}"
}

bot.on('nick-in-use') {
    def origNick = it.original
    def newNick = "${origNick}_"
    println "The nickname ${origNick} is in use. Using ${newNick}"
    bot.changeNick(newNick)
}

timer("Connect") {
    bot.connect()
}
