package ru.otus.dsl.environment

import ru.otus.dsl.config.ServerConfig
import ru.otus.dsl.parser.ConfigurationParser

abstract class BaseEnvironment {
    protected ServerConfig config
    
    BaseEnvironment() {
        config = ConfigurationParser.parseConfig {
            name "MyTest"
            description "Apache Tomcat"
            
            mapping {
                url "/"
                active true
            }
            mapping {
                url "/login"
                active false
            }
        }
    }
    
    abstract void configureEnvironment()
    
    ServerConfig getConfig() {
        return config
    }
}
