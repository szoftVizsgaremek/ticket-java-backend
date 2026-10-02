package com.example.ticketsystem.service;

import java.util.Optional;
import java.util.Set;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.example.ticketsystem.dto.response.UserResponse;
import com.example.ticketsystem.exception.ForbiddenException;
import com.example.ticketsystem.exception.UnauthorizedException;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.repository.UserRepository;

@Service 
public class AuthService {

    public record LoginResult(String token, UserResponse user) {}
    
    private static final Set<String> ADMIN_OR_ABOVE = Set.of("admin", "superadmin");

    private final UserRepository userRepository;
    private final UserService userService;
    private final SessionStore sessionStore;

    public AuthService(UserRepository userRepository, UserService userService, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.sessionStore = sessionStore;
    }

    public User requireUser(String token) {
        if(token == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        Long id = sessionStore.resolve(token);
        
        if (id == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        User user = userRepository.findById(id)
            .orElseThrow( () -> new UnauthorizedException("Not authenticated"));

        return user;
    }

    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (!ADMIN_OR_ABOVE.contains(user.getRole())) {
            throw new ForbiddenException("Forbidden");
        }
        return user;
    }
    public User requireSuperAdmin(String token) {
        User user = requireUser(token);
        if (!"superadmin".equals(user.getRole())) {
            throw new ForbiddenException("Forbidden");
        }
        return user;
    }

    public UserResponse resolveUser(String token) {
        User user = requireUser(token);
        return userService.toResponse((user));
    }

    public LoginResult login(String username, String password){
        Optional<User> userOptional = userRepository.findByUsername(username);
        if(userOptional.isEmpty()) {
            throw new UnauthorizedException("Invalid username or password");
        }
        User user = userOptional.get();
        String safePassword = password == null ? "" : password;
        if(safePassword.isEmpty() || !BCrypt.checkpw(safePassword, user.getHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return new LoginResult(sessionStore.create(user.getId()),userService.toResponse(user));
    }

    public void logout(String token){
        sessionStore.invalidate(token);
    }
}
