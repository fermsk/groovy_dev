package com.example.orm.config

import com.example.orm.meta.EntityClassMetaData
import com.example.orm.meta.EntityClassMetaDataImpl
import com.example.orm.meta.EntitySQLMetaData
import com.example.orm.meta.EntitySQLMetaDataImpl
import com.example.orm.core.DataTemplateJdbc
import com.example.orm.core.DataTemplateJdbcImpl

import java.sql.Connection
import java.util.Properties

class ORMConfig {
    private static Properties properties

    static {
        properties = new Properties()
        def inputStream = ORMConfig.classLoader.getResourceAsStream("database.properties")
        if (inputStream) {
            properties.load(inputStream)
        }
    }

    static Properties getProperties() {
        return properties
    }

    static  DataTemplateJdbc createDataTemplate(Connection connection, Class entityClass) {
        def entityClassMetaData = new EntityClassMetaDataImpl<>(entityClass)
        def entitySQLMetaData = new EntitySQLMetaDataImpl<>(entityClassMetaData)
        return new DataTemplateJdbcImpl<>(connection, entityClassMetaData, entitySQLMetaData)
    }
}