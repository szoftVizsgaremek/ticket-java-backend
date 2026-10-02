package com.example.ticketsystem.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketsystem.dto.request.CreateUserRequest;
import com.example.ticketsystem.dto.request.UpdateUserRequest;
import com.example.ticketsystem.dto.response.UserResponse;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.service.AuthService;
import com.example.ticketsystem.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @CookieValue(value= "session", required = false) String token,
            @RequestBody CreateUserRequest req) {
        User user = authService.requireAdmin(token);
        UserResponse created = userService.create(req, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @CookieValue(value = "session", required = false) String token,
            @PathVariable Long id,
            @RequestBody UpdateUserRequest req
    ) {
        User caller = authService.requireUser(token);
        UserResponse updated = userService.update(id, req, caller);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CookieValue(value = "session", required = false) String token,
            @PathVariable Long id
    ) {
        User caller = authService.requireUser(token);
        userService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list(
            @CookieValue(value= "session", required = false) String token
    ) {
        authService.requireUser(token);
        return ResponseEntity.ok(userService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
            @CookieValue(value = "session", required = false) String token,
            @PathVariable String id) {
        UserResponse currentUser = authService.resolveUser(token);
        
        return "me".equals(id) ?
            ResponseEntity.ok(currentUser) :  
            ResponseEntity.ok(userService.getById(Long.valueOf(id)));
    }
}