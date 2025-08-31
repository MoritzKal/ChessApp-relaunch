package com.chessapp.api.ingest;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class IngestAliasController {

    @PostMapping("/v1/data/import")
    public ResponseEntity<Void> redirect() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .location(URI.create("/v1/ingest"))
                .build();
    }
}
