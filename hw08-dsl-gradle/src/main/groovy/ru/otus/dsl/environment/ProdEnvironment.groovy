package ru.otus.dsl.environment

class ProdEnvironment extends BaseEnvironment {
    @Override
    void configureEnvironment() {
        config.with {
            http {
                port 80
                secure false
            }
            https {
                port 443
                secure true
            }
        }
    }
}
