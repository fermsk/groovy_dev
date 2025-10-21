package ru.otus.dsl.environment

class DevEnvironment extends BaseEnvironment {
    @Override
    void configureEnvironment() {
        config.with {
            http {
                port 8080
                secure false
            }
            https {
                port 4443
                secure true
            }
        }
    }
}
