package com.chessapp.api.account;

import com.chessapp.api.domain.entity.AppUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminUsersController {

    private final AppUserRepository repo;

    public AdminUsersController(AppUserRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Map<String,Object>> list() {
        return repo.findAll().stream().map(u -> Map.of(
                "id", u.getId().toString(),
                "username", u.getUsername(),
                "roles", u.getRoles(),
                "createdAt", u.getCreatedAt() == null ? null : u.getCreatedAt().toString()
        )).toList();
    }

    @PatchMapping(path = "/{id}")
    public Map<String,Object> update(@PathVariable("id") UUID id, @RequestBody Map<String,Object> body) {
        AppUser u = repo.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        Object rolesObj = body.get("roles");
        if (rolesObj instanceof List<?> list) {
            List<String> roles = list.stream().map(Object::toString).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
            if (roles.isEmpty()) roles = List.of("USER");
            u.setRoles(roles);
        }
        repo.save(u);
        return Map.of(
                "id", u.getId().toString(),
                "username", u.getUsername(),
                "roles", u.getRoles(),
                "createdAt", u.getCreatedAt() == null ? null : u.getCreatedAt().toString()
        );
    }
}
