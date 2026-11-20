package com.example.ticketsystem.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticketsystem.exception.NotFoundException;
import com.example.ticketsystem.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    default User findOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    default User findActiveOrThrow(Long id) {
        User user = findOrThrow(id);
        if (!user.isActive()) {
            throw new NotFoundException("User not found");
        }
        return user;
    }

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByActiveTrueOrderByNameAsc();
}
