package com.example.ticketsystem.service;

import java.util.List;
import java.util.Set;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.ticketsystem.dto.request.CreateUserRequest;
import com.example.ticketsystem.dto.response.UserResponse;
import com.example.ticketsystem.exception.BadRequestException;
import com.example.ticketsystem.exception.ConflictException;
import com.example.ticketsystem.exception.NotFoundException;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.repository.UserRepository;

@Service 
public class UserService {

    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{6,}$";

    private static final Set<String> VALID_ROLES = Set.of("user", "area_manager", "admin");
    
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getBirthDate()
        );
    }


    public UserResponse create(CreateUserRequest req) {
        if (req.name() == null || req.name().isBlank() ||
            req.username() == null || req.username().isBlank() ||
            req.email() == null || req.email().isBlank() ||
            req.password() == null || req.password().isBlank()) {
            throw new BadRequestException("Missing required fields");
        }

        if (!req.password().matches(PASSWORD_PATTERN)) {
            throw new BadRequestException("Password must fulfill requirements");
        }

        String role = (req.role() == null) ? "user" : req.role();

        if(!VALID_ROLES.contains(role)) {
            throw new BadRequestException("Invalid role");
        }

        String hash = BCrypt.hashpw(req.password(), BCrypt.gensalt(12));

        User user = new User(
            req.name(),
            req.username(),
            hash,
            req.birthDate(),
            role,
            req.email()
        );

        try {
            User saved = userRepository.save(user);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username and/or email already exists");
        }
    }

    public List<UserResponse> listAll() {
        return userRepository.findAllByOrderByNameAsc()
            .stream()
            .map(user -> toResponse(user))
            .toList();
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
        
        return toResponse(user);
    }
}