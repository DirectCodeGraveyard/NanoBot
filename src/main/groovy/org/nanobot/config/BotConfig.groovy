package org.nanobot.config

import org.nanobot.Utils

class BotConfig {
    File file
    ConfigObject config = new ConfigObject()


    BotConfig(File file) {
        setFile(file)
        load()
    }

    private void initConfig() {
        file.createNewFile()
        config = Utils.parseConfig(file)
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
        config = Utils.parseConfig(file)
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