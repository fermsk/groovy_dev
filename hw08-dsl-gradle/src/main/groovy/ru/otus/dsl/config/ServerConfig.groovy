package ru.otus.dsl.config

class ServerConfig {
    String name
    String description
    HttpConfig http
    HttpsConfig https
    List<MappingConfig> mappings = []
    
    def name(String name) {
        this.name = name
    }
    
    def description(String description) {
        this.description = description
    }
    
    def http(@DelegatesTo(HttpConfig) Closure closure) {
        http = new HttpConfig()
        closure.delegate = http
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
    
    def https(@DelegatesTo(HttpsConfig) Closure closure) {
        https = new HttpsConfig()
        closure.delegate = https
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
    
    def mapping(@DelegatesTo(MappingConfig) Closure closure) {
        def mapping = new MappingConfig()
        closure.delegate = mapping
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        mappings << mapping
    }
}
