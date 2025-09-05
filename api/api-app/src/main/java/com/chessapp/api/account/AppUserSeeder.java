package com.chessapp.api.account;

import com.chessapp.api.domain.entity.AppUser;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppUserSeeder implements ApplicationRunner {

    private final AppUserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AppUserSeeder(AppUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", "admin", List.of("ADMIN"));
        seed("m3ng00s3", "admin", List.of("ADMIN"));
    }

    private void seed(String username, String rawPassword, List<String> roles) {
        repo.findByUsername(username).ifPresentOrElse(
                u -> { /* already exists */ },
                () -> {
                    AppUser u = new AppUser();
                    u.setUsername(username);
                    u.setPasswordHash(encoder.encode(rawPassword));
                    u.setRoles(roles);
                    repo.save(u);
                }
        );
    }
}
