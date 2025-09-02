package com.chessapp.api.models.api;

import com.chessapp.api.domain.entity.Model;
import com.chessapp.api.models.api.dto.ModelDto;
import com.chessapp.api.models.api.dto.ModelPromoteRequest;
import com.chessapp.api.models.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/models")
@Tag(name = "Models")
public class ModelController {
    private final ModelService service;

    public ModelController(ModelService service) {
        this.service = service;
    }

    @Operation(summary = "List available models")
    @GetMapping
    public List<ModelDto> list() {
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Get model details")
    @GetMapping("/{id}")
    public ModelDto get(@PathVariable UUID id) {
        MDC.put("model_id", id.toString());
        try {
            return toDto(service.get(id));
        } finally {
            MDC.remove("model_id");
        }
    }

    @Operation(summary = "Promote model to production",
            requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = "{\n  \"modelId\": \"d290f1ee-6c54-4b01-90e6-d701748f0851\"\n}"))))
    @PostMapping("/promote")
    public ResponseEntity<Void> promote(@org.springframework.web.bind.annotation.RequestBody ModelPromoteRequest req, Authentication auth) {
        MDC.put("model_id", req.modelId().toString());
        service.promote(req.modelId(), auth.getName());
        MDC.remove("model_id");
        return ResponseEntity.ok().build();
    }

    private ModelDto toDto(Model m) {
        return new ModelDto(m.getId(), m.getName(), m.getVersion(), m.getFramework(),
                m.getMetrics(), m.getArtifactUri(), m.isProd(), m.getCreatedAt());
    }
}
