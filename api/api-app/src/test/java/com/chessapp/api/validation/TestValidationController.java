package com.chessapp.api.validation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessapp.api.common.validation.ValidPgn;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/test")
class TestValidationController {
    @PostMapping("/pgn")
    void pgn(@Valid @RequestBody PgnRequest req) {}

    record PgnRequest(@ValidPgn String pgn) {}
}
