package com.example.orm.meta

interface EntitySQLMetaData {
    String getSelectAllSql()
    String getSelectByIdSql()
    String getInsertSql()
    List getFieldNamesWithoutId()
    String getIdFieldName()
    String getTableName()
}