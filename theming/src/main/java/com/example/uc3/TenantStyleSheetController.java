package com.example.uc3;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves each customer's stylesheet, generated from its data, at
 * {@code /tenant-theme/<id>.css}.
 * <p>
 * Gap: {@code Page#addStyleSheet} only takes a URL, and Flow cannot serve a
 * stylesheet it generates itself (there is no stylesheet counterpart of a
 * {@code DownloadHandler}), so a plain Spring endpoint does it.
 */
@RestController
public class TenantStyleSheetController {

    static final MediaType TEXT_CSS = MediaType.valueOf("text/css");

    private final Tenants tenants;

    public TenantStyleSheetController(Tenants tenants) {
        this.tenants = tenants;
    }

    @GetMapping("/tenant-theme/{id}.css")
    public ResponseEntity<String> styleSheet(@PathVariable String id) {
        return tenants.find(id)
                .map(tenant -> ResponseEntity.ok().contentType(TEXT_CSS)
                        .cacheControl(CacheControl.noCache())
                        .body(tenant.styleSheet()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
