package com.example.orm.core

interface DataTemplateJdbc {
    List findAll()
    Optional findById(Long id)
    Long insert(T entity)
    void update(T entity)
    void delete(Long id)
}