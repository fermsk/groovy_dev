package com.example.orm.meta

class EntitySQLMetaDataImpl implements EntitySQLMetaData {
    private final EntityClassMetaData entityClassMetaData
    private final String tableName
    private String selectAllSql
    private String selectByIdSql
    private String insertSql
    private List fieldNamesWithoutId
    private String idFieldName

    EntitySQLMetaDataImpl(EntityClassMetaData entityClassMetaData) {
        this.entityClassMetaData = entityClassMetaData
        this.tableName = convertCamelCaseToSnakeCase(entityClassMetaData.getName()).toLowerCase()
        initializeSQL()
    }

    private void initializeSQL() {
        def fieldNames = entityClassMetaData.allFields.collect { convertCamelCaseToSnakeCase(it.name) }
        def fieldNamesWithoutId = entityClassMetaData.fieldsWithoutId.collect { convertCamelCaseToSnakeCase(it.name) }
        def idFieldName = convertCamelCaseToSnakeCase(entityClassMetaData.idField.name)

        this.selectAllSql = "SELECT ${fieldNames.join(', ')} FROM ${tableName}"
        this.selectByIdSql = "SELECT ${fieldNames.join(', ')} FROM ${tableName} WHERE ${idFieldName} = ?"
        this.insertSql = "INSERT INTO ${tableName} (${fieldNamesWithoutId.join(', ')}) VALUES (${fieldNamesWithoutId.collect { '?' }.join(', ')}) RETURNING ${idFieldName}"
        this.fieldNamesWithoutId = fieldNamesWithoutId
        this.idFieldName = idFieldName
    }

    private String convertCamelCaseToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", '$1_$2').toLowerCase()
    }

    @Override
    String getSelectAllSql() {
        return selectAllSql
    }

    @Override
    String getSelectByIdSql() {
        return selectByIdSql
    }

    @Override
    String getInsertSql() {
        return insertSql
    }

    @Override
    List getFieldNamesWithoutId() {
        return fieldNamesWithoutId
    }

    @Override
    String getIdFieldName() {
        return idFieldName
    }

    @Override
    String getTableName() {
        return tableName
    }
}