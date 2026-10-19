package com.example.usecase28;

import java.io.Serializable;
import java.time.LocalTime;

record LogEntry(LocalTime timestamp, String source, String message,
        Severity severity) implements Serializable {
    enum Severity {
        INFO, WARN, ERROR
    }

    LogEntry withSeverity(Severity newSeverity) {
        return new LogEntry(timestamp, source, message, newSeverity);
    }
}
