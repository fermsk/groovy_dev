package ru.otus.dsl

import ru.otus.dsl.parser.ConfigurationParser
import spock.lang.Specification

class ConfigurationTest extends Specification {
    def "should create valid configuration"() {
        when:
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
        }
        
        then:
        config.name == "MyTest"
        config.description == "Apache Tomcat"
        config.http.port == 8080
        config.http.secure == false
        config.https.port == 4443
        config.https.secure == true
        config.mappings.size() == 1
        config.mappings[0].url == "/"
        config.mappings[0].active == true
    }
}
