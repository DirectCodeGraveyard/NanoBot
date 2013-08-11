import org.nanobot.NanoBot

def bot = new NanoBot('irc.esper.net', 6667, 'GNanoBot')
bot.connect()
bot.on('ready', { ->
    bot.join('#Minetweak')
})