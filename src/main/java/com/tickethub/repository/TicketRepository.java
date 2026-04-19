package com.tickethub.repository;

import com.tickethub.model.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByAuthorId(Long authorId);
}

