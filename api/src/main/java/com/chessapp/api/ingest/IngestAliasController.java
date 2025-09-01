package com.chessapp.api.ingest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Alias – 308 redirect to /v1/ingest",
            description = "Alias – 308 \u2192 /v1/ingest", deprecated = true)
    @ApiResponses({@ApiResponse(responseCode = "308", description = "permanent redirect")})
    public ResponseEntity<Void> redirect() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .location(URI.create("/v1/ingest"))
                .build();
    }
}
