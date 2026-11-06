package com.example.ticketsystem.service;

import java.util.List;
import java.util.Set;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticketsystem.dto.request.CreateUserRequest;
import com.example.ticketsystem.dto.request.UpdateUserRequest;
import com.example.ticketsystem.dto.response.UserResponse;
import com.example.ticketsystem.dto.response.UserSummary;
import com.example.ticketsystem.exception.BadRequestException;
import com.example.ticketsystem.exception.ConflictException;
import com.example.ticketsystem.exception.ForbiddenException;
import com.example.ticketsystem.exception.NotFoundException;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.repository.UserRepository;

@Service 
@Transactional (readOnly = true)
public class UserService {

    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{6,}$";

    private static final Set<String> VALID_ROLES = Set.of("user", "admin", "superadmin");
    
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getBirthDate(),
            user.getRole(),
            user.getJobTitle(),
            user.isActive(),
            user.getEmail()
        );
    }

    public UserSummary toSummary(User user) {
        return new UserSummary(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getJobTitle(),
            user.isActive()
        );
    }

    @Transactional 
    public UserResponse create(CreateUserRequest req, User caller) {
        if (
                req.name() == null || req.name().isBlank() ||
                req.username() == null || req.username().isBlank() ||
                req.email() == null || req.email().isBlank() ||
                req.password() == null || req.password().isBlank() ||
                req.jobTitle() == null || req.jobTitle().isBlank() ||
                req.birthDate() == null
            ) {
            throw new BadRequestException("Missing required fields");
        }

        if (!req.password().matches(PASSWORD_PATTERN)) {
            throw new BadRequestException("Password must fulfill requirements");
        }

        boolean callerIsSuperAdmin = "superadmin".equals(caller.getRole());
        String role = (req.role() == null) ? "user" : req.role();

        if(!VALID_ROLES.contains(role)) {
            throw new BadRequestException("Invalid role");
        }

        if(!callerIsSuperAdmin && !role.equals("user")) {
            throw new ForbiddenException("Forbidden");
        }

        String hash = BCrypt.hashpw(req.password(), BCrypt.gensalt(12));

        User user = new User(
            req.name(),
            req.username(),
            hash,
            req.birthDate(),
            role,
            req.jobTitle(),
            req.email()
        );

        try {
            User saved = userRepository.save(user);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username and/or email already exists");
        }
    }

    @Transactional 
    public UserResponse update(Long targetId, UpdateUserRequest req, User caller) {
        User user = userRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        if(!user.isActive()) {
            throw new NotFoundException("User not found");
        }
        String callerRole = caller.getRole();
        String hash = user.getHash();
        boolean callerIsAdmin = "admin".equals(callerRole);
        boolean callerIsSuperAdmin = "superadmin".equals(callerRole);
        boolean callerIsSelf = targetId.equals(caller.getId());

        if(callerIsSuperAdmin) {
            if (req.name() != null) {
                user.setName(req.name());
            }
            if (req.email() != null) {
                user.setEmail(req.email());
            }
            if (req.jobTitle() != null) {
                user.setJobTitle(req.jobTitle());
            }
            if (req.birthDate() != null) {
                user.setBirthDate(req.birthDate());
            }
            if (req.password() != null) {
                if (req.password().matches(PASSWORD_PATTERN)) {
                    hash = BCrypt.hashpw(req.password(), BCrypt.gensalt(12));
                    user.setHash(hash);
                } else {
                    throw new BadRequestException("Password doesn't fulfill requirements");
                }
            }
            if (req.role() != null ) {
                if (VALID_ROLES.contains(req.role())) {
                    user.setRole(req.role());
                } else {
                    throw new BadRequestException("Role doesn't exist");
                }
            }
            userRepository.save(user);
            return toResponse(user);
        } else if (callerIsSelf) {
            if (
                    req.name() != null && !req.name().isBlank() ||
                    req.email() != null && !req.email().isBlank() ||
                    req.role() != null && !req.role().isBlank() ||
                    req.jobTitle() != null && !req.jobTitle().isBlank() ||
                    req.birthDate() != null
                ) {
                throw new ForbiddenException("Forbidden");
            }

            if (req.password() != null) {
                if (req.password().matches(PASSWORD_PATTERN)) {
                    hash = BCrypt.hashpw(req.password(), BCrypt.gensalt(12));
                    user.setHash(hash);
                    userRepository.save(user);    
                } else {
                    throw new BadRequestException("Password doesn't fulfill requirements");
                }
            }
            return toResponse(user);
            
        } else if(callerIsAdmin && user.getRole().equals("user")) {
            if (req.role() != null) {
                throw new ForbiddenException("Forbidden");    
            }
            if (req.name() != null) {
                user.setName(req.name());
            }
            if (req.email() != null) {
                user.setEmail(req.email());
            }
            if (req.jobTitle() != null) {
                user.setJobTitle(req.jobTitle());
            }
            if (req.birthDate() != null) {
                user.setBirthDate(req.birthDate());
            }
            if (req.password() != null) {
                if (req.password().matches(PASSWORD_PATTERN)) {
                    hash = BCrypt.hashpw(req.password(), BCrypt.gensalt(12));
                    user.setHash(hash);
                } else {
                    throw new BadRequestException("Password doesn't fulfill requirements");
                }
            }
            userRepository.save(user);
            return toResponse(user);
        } else {
            throw new ForbiddenException("Forbidden");
        }
    }

    @Transactional 
    public void delete(Long targetId, User caller) {
        User user = userRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        if(!user.isActive()) {
            throw new NotFoundException("User not found");
        }
        String callerRole = caller.getRole();
        boolean callerIsSuperAdmin = "superadmin".equals(callerRole);
        if (callerIsSuperAdmin) {
            userRepository.delete(user);
        } else {
            throw new ForbiddenException("Forbidden");
        }
    }

    @Transactional 
    public void deactivate(Long targetId, User caller) {
        User user = userRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        if(!user.isActive()) {
            throw new NotFoundException("User not found");
        }
        String callerRole = caller.getRole();
        boolean callerIsAdmin = "admin".equals(callerRole);
        boolean callerIsSuperAdmin = "superadmin".equals(callerRole);
        boolean callerIsSelf = targetId.equals(caller.getId());
        if (callerIsSuperAdmin) {
            user.setActive(false);
            userRepository.save(user);
        } else if (callerIsAdmin) {
            if (callerIsSelf || !user.getRole().equals("user")) {
                throw new ForbiddenException("Forbidden");
            } else {
                user.setActive(false);
                userRepository.save(user);
            }
        } else {
            throw new ForbiddenException("Forbidden");
        }
    }


    public List<UserResponse> listAll() {
        return userRepository.findAllByActiveTrueOrderByNameAsc()
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