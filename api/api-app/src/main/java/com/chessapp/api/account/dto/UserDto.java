package com.chessapp.api.account.dto;

import java.time.Instant;
import java.util.List;

public record UserDto(String id, String username, Instant createdAt, List<String> roles) {}
