package com.example.usecase35;

import java.io.Serializable;

record Card(String title, Priority priority) implements Serializable {
    enum Priority {
        LOW, MEDIUM, HIGH
    }
}
