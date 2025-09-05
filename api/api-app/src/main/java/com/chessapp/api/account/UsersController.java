package com.chessapp.api.account;

import com.chessapp.api.account.dto.UserDto;
import com.chessapp.api.account.dto.UserPrefsDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
public class UsersController {

    private final Map<String, UserPrefsDto> prefs = new ConcurrentHashMap<>();

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        String username = auth.getName();
        List<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(s -> s != null && s.startsWith("ROLE_"))
                .map(s -> s.substring(5))
                .distinct()
                .toList();
        return new UserDto("user-" + username.hashCode(), username, Instant.EPOCH, roles);
    }

    @GetMapping("/me/prefs")
    public UserPrefsDto getPrefs(Authentication auth) {
        return prefs.getOrDefault(auth.getName(), new UserPrefsDto(0.5, 3, "white", true));
    }

    @PutMapping("/me/prefs")
    public UserPrefsDto setPrefs(Authentication auth, @RequestBody UserPrefsDto body) {
        prefs.put(auth.getName(), body);
        return body;
    }
}
