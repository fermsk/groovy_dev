package com.example.model

class Order {
    @Id
    Long id

    String productName
    BigDecimal price
    Long userId

    Order() {}

    Order(String productName, BigDecimal price, Long userId) {
        this.productName = productName
        this.price = price
        this.userId = userId
    }

    @Override
    String toString() {
        return "Order{id=${id}, productName='${productName}', price=${price}, userId=${userId}}"
    }
}