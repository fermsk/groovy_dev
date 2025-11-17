package ru.otus.dsl.environment

class TestEnvironment extends BaseEnvironment {
    @Override
    void configureEnvironment() {
        config.with {
            http {
                port 8081
                secure false
            }
            https {
                port 4443
                secure true
            }
        }
    }
}
