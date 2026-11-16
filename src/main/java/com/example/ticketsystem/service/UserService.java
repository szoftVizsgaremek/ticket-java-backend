package com.example.ticketsystem.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import com.example.ticketsystem.model.AuditAction;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.repository.UserRepository;

@Service 
@Transactional (readOnly = true)
public class UserService {

    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{6,}$";

    private static final Set<String> VALID_ROLES = Set.of("user", "admin", "superadmin");
    private static final Set<String> ADMIN_OR_ABOVE = Set.of("admin", "superadmin");

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SessionStore sessionStore;

    public UserService(UserRepository userRepository, AuditService auditService, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.sessionStore = sessionStore;
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

    private String requireText(String value, int max, String label) {
        String trimmed = value.strip();
        if (trimmed.isEmpty() || trimmed.length() > max) {
            throw new BadRequestException(label + " must be 1-" + max + " characters");
        }
        return trimmed;
    }

    private void applyProfile(User user, UpdateUserRequest req) {
        if (req.name() != null) {
            user.setName(requireText(req.name(), 100, "Name"));
        }
        if (req.email() != null) {
            user.setEmail(requireText(req.email(), 255, "Email"));
        }
        if (req.jobTitle() != null) {
            user.setJobTitle(requireText(req.jobTitle(), 100, "Job title"));
        }
        if (req.birthDate() != null) {
            user.setBirthDate(req.birthDate());
        }
    }

    private void applyPassword(User user, String newPassword) {
        if (newPassword.matches(PASSWORD_PATTERN)) {
            if(BCrypt.checkpw(newPassword, user.getHash())) {
                throw new BadRequestException("New password can't match previous password");
            }
            String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
            user.setHash(hash);
            user.setPasswordChangedAt(Instant.now());
        } else {
            throw new BadRequestException("Password doesn't fulfill requirements");
        }
    }

    private String hashPassword(String raw) {
        if (!raw.matches(PASSWORD_PATTERN) || raw.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password doesn't fulfill requirements");
        }         
        return BCrypt.hashpw(raw, BCrypt.gensalt());
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

        boolean callerIsSuperAdmin = "superadmin".equals(caller.getRole());
        String role = (req.role() == null) ? "user" : req.role();

        if(!VALID_ROLES.contains(role)) {
            throw new BadRequestException("Invalid role");
        }

        if(!callerIsSuperAdmin && !role.equals("user")) {
            throw new ForbiddenException("Forbidden");
        }

        User user = new User(
            requireText(req.name(), 100, "Name"),
            requireText(req.username(), 50, "Username"),
            hashPassword(req.password()),
            req.birthDate(),
            role,
            requireText(req.jobTitle(), 100, "Job title"),
            requireText(req.email(), 255, "Email")
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
        String targetRole = user.getRole();
        boolean callerIsAdmin = "admin".equals(callerRole);
        boolean callerIsSuperAdmin = "superadmin".equals(callerRole);
        boolean callerIsSelf = targetId.equals(caller.getId());

        AuditAction passwordReset = null;
        AuditAction passwordChanged = null;
        AuditAction roleChanged = null;

        if(callerIsSuperAdmin) {
            
            applyProfile(user, req);

            if (req.password() != null) {
                applyPassword(user, req.password());
                if (callerIsSelf) {
                    passwordChanged = AuditAction.PASSWORD_CHANGED;
                } else {
                    passwordReset = AuditAction.PASSWORD_RESET;
                }
                    
            }
            if (req.role() != null ) {
                if (VALID_ROLES.contains(req.role())) {
                    user.setRole(req.role());
                    roleChanged = AuditAction.ROLE_CHANGED;
                } else {
                    throw new BadRequestException("Role doesn't exist");
                }
            }
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
                applyPassword(user, req.password());
                passwordChanged = AuditAction.PASSWORD_CHANGED; 
            }
            
        } else if(callerIsAdmin && user.getRole().equals("user")) {
            if (req.role() != null) {
                throw new ForbiddenException("Forbidden");    
            }
            
            applyProfile(user, req);
            
            if (req.password() != null) {
                applyPassword(user, req.password());
                passwordReset = AuditAction.PASSWORD_RESET;
            }
        } else {
            throw new ForbiddenException("Forbidden");
        }

        try {
            userRepository.saveAndFlush(user);    
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email already exists");
        }
        
        if (passwordChanged != null) {
            auditService.log(caller, user, passwordChanged, null);
        }

        if (passwordReset != null) {
            auditService.log(caller, user, passwordReset, null);
            sessionStore.invalidateAllForUser(targetId);
        }

        if (roleChanged != null && !targetRole.equals(user.getRole())) {
            String details = String.format("%s -> %s", targetRole, user.getRole());
            auditService.log(caller, user, roleChanged, details);
        }
        return toResponse(user);
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
        boolean callerIsSelf = targetId.equals(caller.getId());
        
        if (!ADMIN_OR_ABOVE.contains(callerRole)) {
            throw new ForbiddenException("Forbidden");
        }
        if (callerIsAdmin) {
            if (callerIsSelf || !user.getRole().equals("user")) {
                throw new ForbiddenException("Forbidden");
            } 
        }
        user.setActive(false);
        userRepository.save(user);
        auditService.log(caller, user, AuditAction.USER_DEACTIVATED, null);
        sessionStore.invalidateAllForUser(targetId);
    }

    @Transactional 
    public void reactivate(Long targetId, User caller) {
        User user = userRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if(user.isActive()) {
            throw new ConflictException("User is already active");
        }

        String callerRole = caller.getRole();
        boolean callerIsAdmin = "admin".equals(callerRole);
        boolean callerIsSelf = targetId.equals(caller.getId());

        if (!ADMIN_OR_ABOVE.contains(callerRole)) {
            throw new ForbiddenException("Forbidden");
        }
        if (callerIsAdmin) {
            if (callerIsSelf || !user.getRole().equals("user")) {
                throw new ForbiddenException("Forbidden");
            }
        }

        user.setActive(true);
        userRepository.save(user);
        auditService.log(caller, user, AuditAction.USER_REACTIVATED, null);
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