package org.nanobot.config

import org.nanobot.Utils

class GConfig {
    private File configFile
    private Binding config
    private String defaultConfig

    GConfig(File configFile) {
        this.configFile = configFile
    }

    void setDefaultConfig(String defaultConfig) {
        this.defaultConfig = defaultConfig
    }

    void load() {
        if (!configFile.exists())
            configFile.write(defaultConfig)

        def configScript = Utils.parseConfig(configFile)

        configScript.run()

        this.config = configScript.binding
    }

    @Override
    Object getProperty(String key) {
        if (metaClass.hasProperty(key))
            return metaClass.getMetaProperty(key).getProperty(this)
        else
            return config.getVariable(key)
    }

    Object getProperty(String key, Object defaultValue) {
        if (!hasProperty(key))
            return defaultValue
        else
            return getProperty(key)
    }

    @Override
    void setProperty(String key, Object value) {
        this.config.setVariable(key, value)
    }

    @Override
    boolean hasProperty(String key) {
        return config.hasVariable(key)
    }
}