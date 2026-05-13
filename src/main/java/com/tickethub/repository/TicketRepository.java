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

import java.util.List;

/*
 * COMPARAISON: J2EE CLASSIQUE vs SPRING DATA JPA
 * 
 * Approche sans Spring : 
 * Utilisation de JDBC pur avec des classes DAO. Écriture de requêtes SQL manuelles 
 * (SELECT * FROM tickets...). Gestion des Connection, PreparedStatement et mapping 
 * manuel du ResultSet vers les objets Java.
 * 
 * Différence : 
 * Spring Data JPA génère les requêtes à partir du nom des méthodes (Query Methods) 
 * ou via @Query en JPQL.
 * 
 * Avantage : 
 * Réduction drastique du code répétitif ("boilerplate") et abstraction totale 
 * de la base de données.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {
  @Query("""
      SELECT t
      FROM Ticket t
      WHERE (:statuses IS NULL OR t.status IN :statuses)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:category IS NULL OR t.category = :category)
      """)
  Page<Ticket> findAllWithFilters(
      @Param("statuses") List<TicketStatus> statuses,
      @Param("priority") Priority priority,
      @Param("category") TicketCategory category,
      Pageable pageable);

  @Query("""
      SELECT t
      FROM Ticket t
      WHERE t.author.id = :authorId
        AND (:statuses IS NULL OR t.status IN :statuses)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:category IS NULL OR t.category = :category)
      """)
  Page<Ticket> findByAuthorIdWithFilters(
      @Param("authorId") Long authorId,
      @Param("statuses") List<TicketStatus> statuses,
      @Param("priority") Priority priority,
      @Param("category") TicketCategory category,
      Pageable pageable);

  @Query("""
      SELECT t
      FROM Ticket t
      WHERE t.assignedTechnician.id = :techId
        AND (:statuses IS NULL OR t.status IN :statuses)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:category IS NULL OR t.category = :category)
      """)
  Page<Ticket> findByAssignedTechnicianIdWithFilters(
      @Param("techId") Long techId,
      @Param("statuses") List<TicketStatus> statuses,
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

  @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTechnician.email = :email " +
         "AND t.status = :status AND t.updatedAt >= :startOfDay")
  long countByTechnicianAndStatusAndDate(@Param("email") String email, 
                                         @Param("status") TicketStatus status, 
                                         @Param("startOfDay") java.time.LocalDateTime startOfDay);

  long countByAssignedTechnicianEmailAndStatusIn(String email, List<TicketStatus> statuses);

  long countByAssignedTechnicianEmailAndStatus(String email, TicketStatus status);

  long countByAssignedTechnicianEmailAndPriorityAndStatusIn(String email, Priority priority, List<TicketStatus> statuses);

}
