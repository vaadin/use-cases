package com.example.usecase30;

import java.io.Serializable;

record Product(int id, String name, String category,
        double price) implements Serializable {
}
