package com.example.uc3;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** The customers of the application, standing in for a database table. */
@Component
public class Tenants {

    private final List<Tenant> all = List.of(
            new Tenant("acme", "Acme Logistics", "#c2410c", "#fb923c",
                    "#fff7ed", 2, Tenant.Font.SANS),
            new Tenant("globex", "Globex Bank", "#4338ca", "#a5b4fc", "#eef2ff",
                    0, Tenant.Font.SERIF),
            new Tenant("initech", "Initech Health", "#be185d", "#f472b6",
                    "#fdf2f8", 12, Tenant.Font.ROUNDED));

    public List<Tenant> all() {
        return all;
    }

    public Optional<Tenant> find(String id) {
        return all.stream().filter(t -> t.id().equals(id)).findFirst();
    }
}
