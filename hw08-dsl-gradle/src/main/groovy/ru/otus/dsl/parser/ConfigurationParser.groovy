package ru.otus.dsl.parser

import ru.otus.dsl.config.ServerConfig

class ConfigurationParser {
    static ServerConfig parseConfig(@DelegatesTo(ServerConfig) Closure closure) {
        def config = new ServerConfig()
        closure.delegate = config
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        return config
    }
}
