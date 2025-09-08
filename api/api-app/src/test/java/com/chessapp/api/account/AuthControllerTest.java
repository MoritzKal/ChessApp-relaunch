package com.chessapp.api.account;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.chessapp.api.domain.repo.UserRepository;
import com.chessapp.api.domain.entity.AppUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@TestPropertySource(properties = "app.security.jwt.secret=test-secret")
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AppUserRepository appUserRepository;

    @MockBean
    UserRepository userRepository;

    @Test
    void login() throws Exception {
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        var appUser = new AppUser();
        appUser.setUsername("u");
        appUser.setPasswordHash(encoder.encode("p"));
        when(appUserRepository.findByUsername("u")).thenReturn(Optional.of(appUser));
        when(userRepository.findByChessUsername("u")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        verify(userRepository).save(any());
    }
}
