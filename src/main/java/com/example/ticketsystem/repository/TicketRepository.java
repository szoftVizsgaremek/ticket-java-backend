package com.example.ticketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticketsystem.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long>{
    
    @EntityGraph(attributePaths = {"uploader", "assignee"})
    List<Ticket> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId);
    
    @EntityGraph(attributePaths = {"uploader", "assignee"})
    List<Ticket> findByUploaderIdOrderByCreatedAtDesc(Long uploaderId);

    @EntityGraph(attributePaths = {"uploader", "assignee"})
    List<Ticket> findAllByOrderByCreatedAtDesc();
    
    @Override 
    @EntityGraph(attributePaths = {"uploader", "assignee"})
    Optional<Ticket> findById(Long id);

    @EntityGraph(attributePaths = {"uploader", "assignee"})
    List<Ticket>  findAllByStatusOrderByCreatedAtDesc(String status);
    
    @EntityGraph(attributePaths = {"uploader", "assignee"})
    List<Ticket>  findAllByPriorityOrderByCreatedAtDesc(String priority);
    
    @EntityGraph(attributePaths = {"uploader", "assignee"})
    List<Ticket>  findAllByTypeOrderByCreatedAtDesc(String type);

}
