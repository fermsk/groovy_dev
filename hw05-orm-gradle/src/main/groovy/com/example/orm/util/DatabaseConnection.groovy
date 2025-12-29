package com.example.orm.util

import com.example.orm.config.ORMConfig

import java.sql.Connection
import java.sql.DriverManager

class DatabaseConnection {
    static Connection getConnection() {
        def props = ORMConfig.properties
        def url = props.getProperty("database.url")
        def username = props.getProperty("database.username")
        def password = props.getProperty("database.password")
        def driver = props.getProperty("database.driver")

        Class.forName(driver)
        return DriverManager.getConnection(url, username, password)
    }
}