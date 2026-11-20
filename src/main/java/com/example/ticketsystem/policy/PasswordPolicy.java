package com.example.ticketsystem.policy;

import java.nio.charset.StandardCharsets;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

import com.example.ticketsystem.exception.BadRequestException;

@Component
public class PasswordPolicy {

    private static final String PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{6,}$";
    private static final int MAX_BYTES = 72; 
    private static final int COST = 12;

    public String hash(String raw) {
        validate(raw);
        return BCrypt.hashpw(raw, BCrypt.gensalt(COST));
    }

    public boolean matches(String raw, String hash) {
        if (raw == null || raw.isEmpty() || hash == null || tooLong(raw)) {
            return false;
        }
        return BCrypt.checkpw(raw, hash);
    }

    public void validate(String raw) {
        if (raw == null || !raw.matches(PATTERN) || tooLong(raw)) {
            throw new BadRequestException("Password doesn't fulfill requirements");
        }
    }

    private boolean tooLong(String raw) {
        return raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}