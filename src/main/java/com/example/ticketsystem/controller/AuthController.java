package com.example.ticketsystem.controller;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketsystem.dto.request.LoginRequest;
import com.example.ticketsystem.dto.response.MessageResponse;
import com.example.ticketsystem.dto.response.UserResponse;
import com.example.ticketsystem.service.AuthService;
import com.example.ticketsystem.service.AuthService.LoginResult;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest req) {
        LoginResult res = authService.login(req.username(), req.password());

        ResponseCookie cookie = ResponseCookie.from("session", res.token())
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(7))
            .build();
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(res.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@CookieValue(value = "session", required = false) String token){
        authService.logout(token);

        ResponseCookie clearedCookie = ResponseCookie.from("session", "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
            .body(new MessageResponse("ok"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@CookieValue(value = "session", required = false) String token){
        UserResponse user = authService.resolveUser(token);
        return ResponseEntity.ok(user);
    }
}