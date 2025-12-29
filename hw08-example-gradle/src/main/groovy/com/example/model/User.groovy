package com.example.model

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.FIELD])
@interface Id {}

class User {
    @Id
    Long id

    String name
    String email
    Integer age

    User() {}

    User(String name, String email, Integer age) {
        this.name = name
        this.email = email
        this.age = age
    }

    @Override
    String toString() {
        return "User{id=${id}, name='${name}', email='${email}', age=${age}}"
    }
}