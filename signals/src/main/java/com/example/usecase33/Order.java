package com.example.usecase33;

import java.io.Serializable;

record Order(long id, String customer, String status,
        double total) implements Serializable {
    Order withStatus(String status) {
        return new Order(id, customer, status, total);
    }
}
