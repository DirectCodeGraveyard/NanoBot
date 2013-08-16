package org.nanobot.config

class BotConfig {
    def File file
    private slurper = new ConfigSlurper()
    def config = new ConfigObject()


    BotConfig(File file) {
        setFile(file)
        load()
    }

    private void initConfig() {
        file.createNewFile()
        config = slurper.parse(file.toURI().toURL())
        getServer().get('host', 'irc.esper.net')
        getServer().get('port', 6667)
        getBot().get('nickname', 'SuperNanoBot')
        getBot().get('channels', [])
        save()
    }

    def load() {
        if (!file.exists()) {
            initConfig()
        }
        config = slurper.parse(file.toURI().toURL())
    }

    def save() {
        file.withWriter {
            config.writeTo(it)
        }
    }

    def getServer() {
        return config.get('server', new ConfigObject()) as ConfigObject
    }

    def getBot() {
        return config.get('bot', new ConfigObject()) as ConfigObject
    }
}