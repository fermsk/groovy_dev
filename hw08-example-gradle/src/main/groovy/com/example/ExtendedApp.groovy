package com.example

import com.example.model.Order
import com.example.model.User
import com.example.orm.config.ORMConfig
import com.example.orm.core.DataTemplateJdbc
import com.example.orm.util.DatabaseConnection

import java.sql.Connection

class ExtendedApp {
    static void main(String[] args) {
        Connection connection = null
        try {
            connection = DatabaseConnection.getConnection()

            // Setup database tables
            setupDatabase(connection)

            // Create DataTemplates
            DataTemplateJdbc userTemplate = ORMConfig.createDataTemplate(connection, User)
            DataTemplateJdbc orderTemplate = ORMConfig.createDataTemplate(connection, Order)

            // Insert users
            def user1 = new User("Alice Johnson", "alice@example.com", 28)
            def user2 = new User("Bob Wilson", "bob@example.com", 35)

            def userId1 = userTemplate.insert(user1)
            def userId2 = userTemplate.insert(user2)

            // Insert orders
            def order1 = new Order("Laptop", 1200.00, userId1)
            def order2 = new Order("Mouse", 25.00, userId1)
            def order3 = new Order("Keyboard", 75.00, userId2)

            def orderId1 = orderTemplate.insert(order1)
            def orderId2 = orderTemplate.insert(order2)
            def orderId3 = orderTemplate.insert(order3)

            println "=== Users ==="
            userTemplate.findAll().each { println it }

            println "\n=== Orders ==="
            orderTemplate.findAll().each { println it }

            println "\n=== User Orders ==="
            def user1Orders = orderTemplate.findAll().findAll { it.userId == userId1 }
            user1Orders.each { println it }

        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            connection?.close()
        }
    }

    static void setupDatabase(Connection connection) {
        // Create user table
        def userSql = '''
            CREATE TABLE IF NOT EXISTS user (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255),
                email VARCHAR(255),
                age INTEGER
            )
        '''
        connection.createStatement().execute(userSql)

        // Create order table
        def orderSql = '''
            CREATE TABLE IF NOT EXISTS order (
                id BIGSERIAL PRIMARY KEY,
                product_name VARCHAR(255),
                price DECIMAL(10, 2),
                user_id BIGINT
            )
        '''
        connection.createStatement().execute(orderSql)
    }
}