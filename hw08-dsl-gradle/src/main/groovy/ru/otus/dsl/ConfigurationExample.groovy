package ru.otus.dsl

import ru.otus.dsl.environment.DevEnvironment
import ru.otus.dsl.environment.ProdEnvironment
import ru.otus.dsl.environment.TestEnvironment
import ru.otus.dsl.parser.ConfigurationParser

// Base configuration example
def config = ConfigurationParser.parseConfig {
    name "MyTest"
    description "Apache Tomcat"
    
    http {
        port 8080
        secure false
    }
    
    https {
        port 4443
        secure true
    }
    
    mapping {
        url "/"
        active true
    }
    mapping {
        url "/login"
        active false
    }
}

// Environment-specific configurations
def devConfig = new DevEnvironment()
devConfig.configureEnvironment()

def prodConfig = new ProdEnvironment()
prodConfig.configureEnvironment()

def testConfig = new TestEnvironment()
testConfig.configureEnvironment()

println "Base configuration:"
println config.dump()

println "\nDev configuration:"
println devConfig.config.dump()

println "\nProd configuration:"
println prodConfig.config.dump()

println "\nTest configuration:"
println testConfig.config.dump()
