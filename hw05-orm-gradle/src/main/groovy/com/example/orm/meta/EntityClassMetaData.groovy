package com.example.orm.meta

import java.lang.reflect.Field

interface EntityClassMetaData {
    String getName()
    Field getIdField()
    List getAllFields()
    List getFieldsWithoutId()
    Class getEntityClass()
}