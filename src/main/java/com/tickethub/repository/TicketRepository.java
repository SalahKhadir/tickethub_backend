package com.tickethub.repository;

import com.tickethub.model.Priority;
import com.tickethub.model.Ticket;
import com.tickethub.model.TicketCategory;
import com.tickethub.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query("""
            SELECT t
            FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """)
    Page<Ticket> findAllWithFilters(
            @Param("status") TicketStatus status,
            @Param("priority") Priority priority,
            @Param("category") TicketCategory category,
            Pageable pageable);

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.author.id = :authorId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """)
    Page<Ticket> findByAuthorIdWithFilters(
            @Param("authorId") Long authorId,
            @Param("status") TicketStatus status,
            @Param("priority") Priority priority,
            @Param("category") TicketCategory category,
            Pageable pageable);

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.assignedTechnician.id = :techId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """)
    Page<Ticket> findByAssignedTechnicianIdWithFilters(
            @Param("techId") Long techId,
            @Param("status") TicketStatus status,
            @Param("priority") Priority priority,
            @Param("category") TicketCategory category,
            Pageable pageable);

    long countByAssignedTechnicianIdAndStatusIn(Long techId, java.util.List<TicketStatus> statuses);

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.slaDeadline BETWEEN :now AND :threshold
              AND t.status NOT IN :excludedStatuses
            """)
    java.util.List<Ticket> findTicketsNearingSla(
            @Param("now") java.time.LocalDateTime now,
            @Param("threshold") java.time.LocalDateTime threshold,
            @Param("excludedStatuses") java.util.List<TicketStatus> excludedStatuses);
}
