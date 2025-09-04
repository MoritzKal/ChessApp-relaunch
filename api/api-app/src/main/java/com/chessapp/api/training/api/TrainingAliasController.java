package com.chessapp.api.training.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TrainingAliasController {

    @GetMapping("/v1/training/{runId}")
    public String alias(@PathVariable String runId) {
        return "forward:/v1/trainings/" + runId;
    }
}
