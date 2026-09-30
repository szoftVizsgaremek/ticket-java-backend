package com.example.ticketsystem.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticketsystem.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByOrderByNameAsc();
}
