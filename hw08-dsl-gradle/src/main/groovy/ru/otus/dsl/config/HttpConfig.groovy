package ru.otus.dsl.config

class HttpConfig {
    int port
    boolean secure
    
    def port(int port) {
        this.port = port
    }
    
    def secure(boolean secure) {
        this.secure = secure
    }
}
