package com.example.orm.meta

import java.lang.reflect.Field

class EntityClassMetaDataImpl implements EntityClassMetaData {
    private final Class entityClass
    private Field idField
    private List allFields
    private List fieldsWithoutId

    EntityClassMetaDataImpl(Class entityClass) {
        this.entityClass = entityClass
        initializeFields()
    }

    private void initializeFields() {
        allFields = entityClass.declaredFields.findAll { field ->
            field annotations any { it.annotationType().simpleName == 'Id' }
        }

        idField = allFields.find { field ->
            field annotations any { it.annotationType().simpleName == 'Id' }
        }

        if (!idField) {
            throw new IllegalStateException("Entity class ${entityClass.name} must have an @Id field")
        }

        fieldsWithoutId = allFields.findAll { it != idField }
    }

    @Override
    String getName() {
        return entityClass.simpleName
    }

    @Override
    Field getIdField() {
        idField.accessible = true
        return idField
    }

    @Override
    List getAllFields() {
        allFields.each { it.accessible = true }
        return allFields
    }

    @Override
    List getFieldsWithoutId() {
        fieldsWithoutId.each { it.accessible = true }
        return fieldsWithoutId
    }

    @Override
    Class getEntityClass() {
        return entityClass
    }
}