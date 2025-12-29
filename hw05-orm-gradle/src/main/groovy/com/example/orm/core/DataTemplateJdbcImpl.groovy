package com.example.orm.core

import com.example.orm.meta.EntityClassMetaData
import com.example.orm.meta.EntitySQLMetaData
import groovy.sql.Sql

import java.sql.Connection
import java.sql.PreparedStatement

class DataTemplateJdbcImpl implements DataTemplateJdbc {
    private final Connection connection
    private final EntityClassMetaData entityClassMetaData
    private final EntitySQLMetaData entitySQLMetaData

    DataTemplateJdbcImpl(Connection connection, EntityClassMetaData entityClassMetaData, EntitySQLMetaData entitySQLMetaData) {
        this.connection = connection
        this.entityClassMetaData = entityClassMetaData
        this.entitySQLMetaData = entitySQLMetaData
    }

    @Override
    List findAll() {
        def sql = new Sql(connection)
        def entities = []

        sql.eachRow(entitySQLMetaData.selectAllSql) { row ->
            entities.add(mapRowToEntity(row))
        }

        return entities
    }

    @Override
    Optional findById(Long id) {
        def sql = new Sql(connection)
        def entity = null

        sql.eachRow(entitySQLMetaData.selectByIdSql, [id]) { row ->
            entity = mapRowToEntity(row)
        }

        return Optional.ofNullable(entity)
    }

    @Override
    Long insert(T entity) {
        def preparedStatement = connection.prepareStatement(entitySQLMetaData.insertSql, PreparedStatement.RETURN_GENERATED_KEYS)

        def fieldsWithoutId = entityClassMetaData.fieldsWithoutId
        for (int i = 0; i < fieldsWithoutId.size(); i++) {
            def field = fieldsWithoutId[i]
            def value = field.get(entity)
            preparedStatement.setObject(i + 1, value)
        }

        preparedStatement.executeUpdate()

        def generatedKeys = preparedStatement.getGeneratedKeys()
        if (generatedKeys.next()) {
            return generatedKeys.getLong(1)
        } else {
            throw new IllegalStateException("Unable to retrieve generated key")
        }
    }

    @Override
    void update(T entity) {
        // Implementation for update operation
        def idValue = entityClassMetaData.idField.get(entity)
        if (!idValue) {
            throw new IllegalArgumentException("Entity must have an ID to update")
        }

        def fieldNames = entityClassMetaData.fieldsWithoutId.collect {
            entitySQLMetaData.fieldNamesWithoutId[entityClassMetaData.fieldsWithoutId.indexOf(it)]
        }

        def setClause = fieldNames.collect { "${it} = ?" }.join(', ')
        def sql = "UPDATE ${entitySQLMetaData.tableName} SET ${setClause} WHERE ${entitySQLMetaData.idFieldName} = ?"

        def preparedStatement = connection.prepareStatement(sql)

        def fieldsWithoutId = entityClassMetaData.fieldsWithoutId
        for (int i = 0; i < fieldsWithoutId.size(); i++) {
            def field = fieldsWithoutId[i]
            def value = field.get(entity)
            preparedStatement.setObject(i + 1, value)
        }

        preparedStatement.setObject(fieldsWithoutId.size() + 1, idValue)
        preparedStatement.executeUpdate()
    }

    @Override
    void delete(Long id) {
        def sql = "DELETE FROM ${entitySQLMetaData.tableName} WHERE ${entitySQLMetaData.idFieldName} = ?"
        def preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setLong(1, id)
        preparedStatement.executeUpdate()
    }

    private T mapRowToEntity(row) {
        def entity = entityClassMetaData.entityClass.newInstance()

        entityClassMetaData.allFields.each { field ->
            def columnName = entitySQLMetaData.fieldNamesWithoutId.find {
                it == convertCamelCaseToSnakeCase(field.name)
            } ?: entitySQLMetaData.idFieldName

            if (columnName == convertCamelCaseToSnakeCase(field.name) ||
                    (field == entityClassMetaData.idField && columnName == entitySQLMetaData.idFieldName)) {
                def value = row."${columnName}"
                field.set(entity, value)
            }
        }

        return entity
    }

    private String convertCamelCaseToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", '$1_$2').toLowerCase()
    }
}