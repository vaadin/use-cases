package com.example.usecase29;

import java.io.Serializable;
import java.time.LocalTime;

record AuditEntry(LocalTime time, String admin,
        String changeSummary) implements Serializable {
}
