package com.example.ticketsystem.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketsystem.dto.request.CreateTicketRequest;
import com.example.ticketsystem.dto.request.UpdateTicketRequest;
import com.example.ticketsystem.dto.response.TicketResponse;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.service.AuthService;
import com.example.ticketsystem.service.TicketService;

@RestController 
@RequestMapping("/api/tickets")
public class TicketController {
    
    private final TicketService ticketService;
    private final AuthService authService;

    public TicketController(TicketService ticketService, AuthService authService) {
        this.ticketService = ticketService;
        this.authService = authService;
    }

    @PostMapping 
    public ResponseEntity<TicketResponse> create (
            @CookieValue(value = "session", required = false) String token,
            @RequestBody CreateTicketRequest req
    ) {
        User caller = authService.requireUser(token);
        TicketResponse created = ticketService.create(req, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> update (
            @CookieValue(value = "session", required = false) String token,
            @PathVariable Long id,
            @RequestBody UpdateTicketRequest req
    ) {
        User caller = authService.requireUser(token);
        TicketResponse updated = ticketService.update(id, req, caller);
        return ResponseEntity.ok(updated);
    }

    @GetMapping 
    public ResponseEntity<List<TicketResponse>> list(
            @CookieValue(value = "session", required = false) String token
    ) {
        authService.requireUser(token);
        return ResponseEntity.ok(ticketService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(
            @CookieValue(value = "session", required = false) String token,
            @PathVariable Long id
    ) {
        authService.requireUser(token);
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @GetMapping("/assigned-to/{userId}")
    public ResponseEntity<List<TicketResponse>> listAssignedTo(
            @CookieValue(value = "session", required = false) String token,
            @PathVariable Long userId
    ) {
        authService.requireUser(token);
        return ResponseEntity.ok(ticketService.listAssignedTo(userId));
    }
    @GetMapping("/opened-by/{userId}")
    public ResponseEntity<List<TicketResponse>> listOpenedBy(
            @CookieValue(value = "session", required = false) String token,
            @PathVariable Long userId
    ) {
        authService.requireUser(token);
        return ResponseEntity.ok(ticketService.listOpenedBy(userId));
    }
}
