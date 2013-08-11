package org.nanobot

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
        thread.setName('NanoBot-Input')
        thread.start()
        writer.println 'NICK ' + bot.nickname
        writer.println 'USER NanoBot * 8 :NanoBot'
    }

    @Override
    def void run() {
        def line
        while ((line = reader.readLine()) != null) {
            def split = line.split(' ')
            println line
            if (split[0].equals('PING')) {
                bot.dispatch('ping', split[1].substring(1))
                writer.println 'PONG ' + split[1]
                if (!ready) {
                    ready = true
                    bot.dispatch('ready')
                }
            }
        }
    }
}
