package com.example.ticketsystem.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.ticketsystem.dto.request.CreateTicketRequest;
import com.example.ticketsystem.dto.request.UpdateTicketRequest;
import com.example.ticketsystem.dto.response.TicketResponse;
import com.example.ticketsystem.exception.BadRequestException;
import com.example.ticketsystem.exception.ForbiddenException;
import com.example.ticketsystem.exception.NotFoundException;
import com.example.ticketsystem.model.Ticket;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.repository.TicketRepository;
import com.example.ticketsystem.repository.UserRepository;

@Service 
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    private final Set<String> VALID_TYPES = Set.of("thermal", "software", "hardware");
    private final Set<String> VALID_PRIORITIES = Set.of("low", "moderate", "high", "critical");
    private final Set<String> VALID_STATUSES = Set.of("open", "in_progress", "closed");

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository, UserService userService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public TicketResponse toResponse(Ticket ticket) {
        User uploader = userRepository.findById(ticket.getUploadedBy())
            .orElseThrow(() -> new IllegalStateException("Ticket references a non-existent user"));
        User assignee = userRepository.findById(ticket.getAssigneeId())
            .orElseThrow(() -> new IllegalStateException("Ticket references a non-existent user"));
        return new TicketResponse(
            ticket.getId(),
            ticket.getName(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getPriority(),
            ticket.getType(),
            userService.toSummary(uploader),
            userService.toSummary(assignee),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt()
        );
    }

    public TicketResponse create(CreateTicketRequest req, User caller) {
        if (
                req.name() == null || req.name().isBlank() ||
                req.description() == null || req.description().isBlank() ||
                req.type() == null || req.type().isBlank() ||
                req.assigneeId() == null
            ) {
            throw new BadRequestException("Missing required fields");
        }
        String type = req.type();
        if (!VALID_TYPES.contains(type)) {
            throw new BadRequestException("Must be a valid type");
        }
        String priority = req.priority();      
        if (priority == null || req.priority().isBlank()) {
            priority = "low";
        }
        if (!VALID_PRIORITIES.contains(priority)) {
            throw new BadRequestException("Must be a valid priority label");
        }

        User assignee = userRepository.findById(req.assigneeId())
            .orElseThrow(() -> new BadRequestException("Assignee is a non-existent user"));
        Ticket ticket = new Ticket(
            req.name(),
            req.description(),
            "open",
            priority,
            type,
            caller.getId(),
            assignee.getId()
        );

        Ticket saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    public TicketResponse update(Long targetId, UpdateTicketRequest req, User caller) {
        Ticket ticket = ticketRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("Ticket not found"));
        
        Long uploaderId = ticket.getUploadedBy();
        User uploader = userRepository.findById(uploaderId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
        
        Long currentAssigneeId = ticket.getAssigneeId();
        User currentAssignee = userRepository.findById(currentAssigneeId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
            
        String callerJobTitle = caller.getJobTitle();
        String uploaderJobTitle = uploader.getJobTitle();
        String currentAssigneeJobTitle = currentAssignee.getJobTitle();
        boolean callerIsSuperAdmin = "superadmin".equals(caller.getRole());

        boolean sameField = callerJobTitle.equals(uploaderJobTitle) 
                            || callerJobTitle.equals(currentAssigneeJobTitle);
        if (!callerIsSuperAdmin && !sameField) {
            throw new ForbiddenException("Forbidden");
        }
    
        if (req.status() != null) {
            if (VALID_STATUSES.contains(req.status())) {
                ticket.setStatus(req.status());    
            } else {
                throw new BadRequestException("Must be a valid status label");
            }
            
        }
        if (req.priority() != null) {
            if (VALID_PRIORITIES.contains(req.priority())) {
                ticket.setPriority(req.priority());    
            } else {
                throw new BadRequestException("Must be a valid priority label");
            }
        }
        if (req.assigneeId() != null) {
            User newAssignee = userRepository.findById(req.assigneeId())
                .orElseThrow(() -> new BadRequestException("User doesn't exist"));
            ticket.setAssigneeId(newAssignee.getId());
        }
        if (req.type() != null) {
            if (VALID_TYPES.contains(req.type())) {
                ticket.setType(req.type());    
            } else {
                throw new BadRequestException("Must be a valid type");
            }
        }

        Ticket saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    public TicketResponse getById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Ticket not found"));
        return toResponse(ticket);
    }

    

    public List<TicketResponse> listAll() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(ticket -> toResponse(ticket))
            .toList();
    }

    public List<TicketResponse> listOpenedBy(Long id) {
        return ticketRepository.findByUploadedBy(id)
            .stream()
            .map(ticket -> toResponse(ticket))
            .toList();
    }

    public List<TicketResponse> listAssignedTo(Long id) {
        return ticketRepository.findByAssigneeId(id)
            .stream()
            .map(ticket -> toResponse(ticket))
            .toList();
    }
}
