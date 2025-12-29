package com.example

import com.example.model.User
import com.example.orm.config.ORMConfig
import com.example.orm.core.DataTemplateJdbc
import com.example.orm.util.DatabaseConnection

import java.sql.Connection

class App {
    static void main(String[] args) {
        Connection connection = null
        try {
            connection = DatabaseConnection.getConnection()

            // Create DataTemplate for User entity
            DataTemplateJdbc userTemplate = ORMConfig.createDataTemplate(connection, User)

            // Create table (for demo purposes)
            setupDatabase(connection)

            // Insert users
            def user1 = new User("John Doe", "john@example.com", 30)
            def user2 = new User("Jane Smith", "jane@example.com", 25)

            def id1 = userTemplate.insert(user1)
            def id2 = userTemplate.insert(user2)

            println "Inserted users with IDs: ${id1}, ${id2}"

            // Find all users
            def allUsers = userTemplate.findAll()
            println "All users: ${allUsers}"

            // Find user by ID
            def foundUser = userTemplate.findById(id1)
            if (foundUser.isPresent()) {
                println "Found user: ${foundUser.get()}"
            }

            // Update user
            def userToUpdate = foundUser.get()
            userToUpdate.age = 31
            userTemplate.update(userToUpdate)
            println "Updated user: ${userToUpdate}"

            // Delete user
            userTemplate.delete(id2)
            println "Deleted user with ID: ${id2}"

            // Show remaining users
            def remainingUsers = userTemplate.findAll()
            println "Remaining users: ${remainingUsers}"

        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            connection?.close()
        }
    }

    static void setupDatabase(Connection connection) {
        def sql = '''
            CREATE TABLE IF NOT EXISTS user (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255),
                email VARCHAR(255),
                age INTEGER
            )
        '''
        connection.createStatement().execute(sql)
    }
}