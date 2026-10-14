package com.example.ticketsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticketsystem.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long>{
    
    List<Ticket> findByAssigneeId(Long assigneeId);
    
    List<Ticket> findByUploadedBy(Long uploadedBy);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket>  findAllByStatusOrderByCreatedAtDesc(String status);
    
    List<Ticket>  findAllByPriorityOrderByCreatedAtDesc(String priority);
    
    List<Ticket>  findAllByTypeOrderByCreatedAtDesc(String type);

}
