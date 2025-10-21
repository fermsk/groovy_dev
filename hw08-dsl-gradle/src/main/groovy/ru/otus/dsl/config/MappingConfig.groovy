package ru.otus.dsl.config

class MappingConfig {
    String url
    boolean active
    
    def url(String url) {
        this.url = url
    }
    
    def active(boolean active) {
        this.active = active
    }
}
