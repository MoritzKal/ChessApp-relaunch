package com.chessapp.api.models.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Model promotion request")
public record ModelPromoteRequest(
        @Schema(example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID modelId
) {}
