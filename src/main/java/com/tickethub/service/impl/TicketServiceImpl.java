package com.tickethub.service.impl;

import com.tickethub.dto.request.TicketRequest;
import com.tickethub.dto.request.TicketUpdateRequest;
import com.tickethub.dto.response.TicketResponse;
import com.tickethub.dto.response.TechnicianStatsResponse;
import com.tickethub.exception.ForbiddenOperationException;
import com.tickethub.model.Priority;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.model.Role;
import com.tickethub.model.Ticket;
import com.tickethub.model.TicketCategory;
import com.tickethub.model.TicketStatus;
import com.tickethub.model.User;
import com.tickethub.repository.TicketRepository;
import com.tickethub.repository.UserRepository;
import com.tickethub.service.TicketService;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * COMPARAISON: J2EE CLASSIQUE vs SPRING BOOT (Services)
 * 
 * Approche sans Spring : 
 * Gestion manuelle des transactions via connection.setAutoCommit(false) et 
 * connection.commit(). Instanciation manuelle des classes (pas d'Injection de Dépendances).
 * 
 * Avantage Spring : 
 * L'Inversion de Contrôle (IoC) et l'Injection de Dépendances (DI) rendent le code 
 * modulaire, testable et découplé. L'annotation @Transactional gère les commits et rollbacks.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {
    private static final Set<String> CLIENT_AUTHORITIES = Set.of("ROLE_CLIENT");
    private static final Set<String> STAFF_AUTHORITIES = Set.of("ROLE_TECH", "ROLE_ADMIN");
    private static final Set<String> ADMIN_AUTHORITIES = Set.of("ROLE_ADMIN");
    private static final Set<String> TECH_AUTHORITIES = Set.of("ROLE_TECH");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final com.tickethub.service.NotificationPushService notificationPushService;

    @Override
    public TicketResponse createTicket(TicketRequest request) {
        User currentUser = getCurrentUser();
        LocalDateTime slaDeadline = request.priority() == Priority.CRITICAL
                ? LocalDateTime.now().plusHours(2)
                : null;

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .description(request.description())
                .status(TicketStatus.NEW)
                .priority(request.priority())
                .category(request.category())
                .slaDeadline(slaDeadline)
                .author(currentUser)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        TicketResponse response = toResponse(savedTicket);

        notificationPushService.broadcastToAdmins(response);

        return response;
    }

    @Override
    public Page<TicketResponse> getAllTickets(
            Pageable pageable,
            String statusString,
            String priorityString,
            String categoryString) {

        List<TicketStatus> statusList = null;
        if (statusString != null && !statusString.isBlank()) {
            statusList = Arrays.stream(statusString.split(","))
                    .map(String::trim)
                    .map(s -> {
                        try {
                            return TicketStatus.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            if (statusList.isEmpty()) {
                statusList = null;
            }
        }

        Priority priority = null;
        if (priorityString != null && !priorityString.isBlank()) {
            try {
                priority = Priority.valueOf(priorityString.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid
            }
        }

        TicketCategory category = null;
        if (categoryString != null && !categoryString.isBlank()) {
            try {
                category = TicketCategory.valueOf(categoryString.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid
            }
        }

        Authentication authentication = getCurrentAuthentication();
        boolean isAdmin = hasAnyAuthority(authentication, ADMIN_AUTHORITIES);
        boolean isTech = hasAnyAuthority(authentication, TECH_AUTHORITIES);

        Page<Ticket> tickets;
        if (isAdmin) {
            tickets = ticketRepository.findAllWithFilters(statusList, priority, category, pageable);
        } else if (isTech) {
            User currentUser = getCurrentUser();
            tickets = ticketRepository.findByAssignedTechnicianIdWithFilters(
                    currentUser.getId(),
                    statusList,
                    priority,
                    category,
                    pageable);
        } else if (hasAnyAuthority(authentication, CLIENT_AUTHORITIES)) {
            User currentUser = getCurrentUser();
            tickets = ticketRepository.findByAuthorIdWithFilters(
                    currentUser.getId(),
                    statusList,
                    priority,
                    category,
                    pageable);
        } else {
            throw new ForbiddenOperationException("You are not allowed to access tickets.");
        }

        return tickets.map(this::toResponse);
    }

    @Override
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = findTicketByIdOrThrow(id);
        Authentication authentication = getCurrentAuthentication();

        // Critical security check: only author or staff can access the ticket.
        boolean isStaff = hasAnyAuthority(authentication, STAFF_AUTHORITIES);
        boolean isAuthor = ticket.getAuthor().getEmail().equalsIgnoreCase(authentication.getName());
        if (!isAuthor && !isStaff) {
            throw new AccessDeniedException("You are not allowed to access this ticket.");
        }

        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse assignTechnician(Long ticketId, Long techId) {
        Ticket ticket = findTicketByIdOrThrow(ticketId);
        Authentication authentication = getCurrentAuthentication();

        if (!hasAnyAuthority(authentication, ADMIN_AUTHORITIES)) {
            throw new AccessDeniedException("Only ADMIN can assign a technician.");
        }

        if (ticket.getStatus() != TicketStatus.ACCEPTED) {
            throw new IllegalStateException("Ticket must be ACCEPTED before assigning a technician.");
        }

        User technician = userRepository.findById(techId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + techId));

        if (!technician.getRoles().contains(Role.ROLE_TECH)) {
            throw new IllegalStateException("Assigned user must have role TECHNICIAN.");
        }

        ticket.setAssignedTechnician(technician);
        // Status stays ACCEPTED — technician must click "Start Work" to move to IN_PROGRESS
        Ticket updatedTicket = ticketRepository.save(ticket);
        TicketResponse response = toResponse(updatedTicket);

        notificationPushService.push(technician.getEmail(), response);

        return response;
    }

    @Override
    public TicketResponse updateTicketStatus(Long id, TicketStatus newStatus, String solution) {
        Ticket ticket = findTicketByIdOrThrow(id);
        Authentication authentication = getCurrentAuthentication();

        TicketStatus currentStatus = ticket.getStatus();
        String currentUserEmail = authentication.getName();
        boolean isClient = hasAnyAuthority(authentication, CLIENT_AUTHORITIES);
        boolean isAuthor = ticket.getAuthor().getEmail().equalsIgnoreCase(currentUserEmail);

        if (currentStatus == TicketStatus.NEW && newStatus == TicketStatus.ACCEPTED) {
            if (!hasAnyAuthority(authentication, Set.of("ROLE_ADMIN"))) {
                throw new AccessDeniedException("Only ADMIN can accept a ticket.");
            }
        } else if (currentStatus == TicketStatus.ACCEPTED && newStatus == TicketStatus.IN_PROGRESS) {
            if (!hasAnyAuthority(authentication, Set.of("ROLE_TECH"))) {
                throw new AccessDeniedException("Only TECH can start work on a ticket.");
            }
            User currentUser = getCurrentUser();
            if (ticket.getAssignedTechnician() == null
                    || !ticket.getAssignedTechnician().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Only the assigned technician can start work on this ticket.");
            }
        } else if (currentStatus == TicketStatus.IN_PROGRESS && newStatus == TicketStatus.RESOLVED) {
            if (!hasAnyAuthority(authentication, Set.of("ROLE_TECH"))) {
                throw new AccessDeniedException("Only TECH can resolve a ticket.");
            }
            User currentUser = getCurrentUser();
            if (ticket.getAssignedTechnician() == null
                    || !ticket.getAssignedTechnician().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Only the assigned technician can resolve this ticket.");
            }
            if (solution == null || solution.isBlank()) {
                throw new IllegalArgumentException("Solution is required to resolve a ticket.");
            }
            ticket.setSolution(solution);
        } else if (currentStatus == TicketStatus.RESOLVED && newStatus == TicketStatus.CLOSED) {
            if (!(isClient && isAuthor)) {
                throw new AccessDeniedException("Only the CLIENT author can close a ticket.");
            }
        } else {
            throw new IllegalStateException(
                    "Invalid transition: " + currentStatus + " -> " + newStatus);
        }

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);
        TicketResponse response = toResponse(updatedTicket);

        if (newStatus == TicketStatus.RESOLVED) {
            notificationPushService.push(ticket.getAuthor().getEmail(), response);
            notificationPushService.broadcastToAdmins(response);
        } else if (newStatus == TicketStatus.IN_PROGRESS) {
            notificationPushService.push(ticket.getAuthor().getEmail(), response);
            notificationPushService.broadcastToAdmins(response);
        }

        return response;
    }

    @Override
    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = findTicketByIdOrThrow(id);
        Authentication authentication = getCurrentAuthentication();

        // Critical security check: delete requires author ownership or staff role.
        boolean isStaff = hasAnyAuthority(authentication, STAFF_AUTHORITIES);
        boolean isClient = hasAnyAuthority(authentication, CLIENT_AUTHORITIES);
        boolean isAuthor = ticket.getAuthor().getEmail().equalsIgnoreCase(authentication.getName());

        if (!isAuthor && !isStaff) {
            throw new AccessDeniedException("You are not allowed to delete this ticket.");
        }

        if (isClient && ticket.getStatus() != TicketStatus.NEW) {
            throw new AccessDeniedException("A client can only delete a NEW ticket.");
        }

        // Clear assignment before delete to avoid FK constraint
        ticket.setAssignedTechnician(null);
        ticketRepository.saveAndFlush(ticket);
        ticketRepository.delete(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long id, TicketUpdateRequest request) {
        Ticket ticket = findTicketByIdOrThrow(id);
        Authentication authentication = getCurrentAuthentication();

        boolean isStaff = hasAnyAuthority(authentication, STAFF_AUTHORITIES);
        boolean isAuthor = ticket.getAuthor().getEmail()
                .equalsIgnoreCase(authentication.getName());

        if (!isAuthor && !isStaff) {
            throw new AccessDeniedException("You are not allowed to edit this ticket.");
        }
        if (isAuthor && !isStaff && ticket.getStatus() != TicketStatus.NEW) {
            throw new AccessDeniedException("You can only edit a ticket with status NEW.");
        }
        if (request.title() != null && !request.title().isBlank()) {
            ticket.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(request.description().trim());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
            if (request.priority() == Priority.CRITICAL) {
                ticket.setSlaDeadline(LocalDateTime.now().plusHours(2));
            } else {
                ticket.setSlaDeadline(null);
            }
        }
        return toResponse(ticketRepository.save(ticket));
    }

    @Override
    public com.tickethub.dto.response.TechnicianStatsResponse getTechnicianStats(String email) {
        /*
         * COMPARAISON: J2EE CLASSIQUE vs SPRING DATA JPA (Statistiques)
         * 
         * En JDBC classique, il aurait fallu écrire 4 requêtes SELECT COUNT(*) distinctes 
         * avec des jointures explicites pour vérifier l'email de l'utilisateur. 
         * Avec Spring Data JPA, de simples requêtes basées sur le nom (Query Methods) 
         * ou une courte @Query suffisent pour abstraire cette complexité d'un coup.
         */
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().with(java.time.LocalTime.MIN);

        long assignedTickets = ticketRepository.countByAssignedTechnicianEmailAndStatusIn(
                email, List.of(TicketStatus.ACCEPTED, TicketStatus.IN_PROGRESS));

        long inProgress = ticketRepository.countByAssignedTechnicianEmailAndStatus(
                email, TicketStatus.IN_PROGRESS);

        long criticalPriority = ticketRepository.countByAssignedTechnicianEmailAndPriorityAndStatusIn(
                email, Priority.CRITICAL, List.of(TicketStatus.ACCEPTED, TicketStatus.IN_PROGRESS));

        long resolvedToday = ticketRepository.countByTechnicianAndStatusAndDate(
                email, TicketStatus.RESOLVED, startOfDay);

        return new com.tickethub.dto.response.TechnicianStatsResponse(
                assignedTickets, inProgress, criticalPriority, resolvedToday);
    }

    @Override
    public com.tickethub.dto.response.AdminStatsResponse getAdminGlobalStats() {
        Authentication authentication = getCurrentAuthentication();
        if (!hasAnyAuthority(authentication, ADMIN_AUTHORITIES)) {
            throw new AccessDeniedException("Only ADMIN can view global stats.");
        }

        /*
         * COMPARAISON PÉDAGOGIQUE: RAPPORTS JDBC vs SPRING DATA JPA
         *
         * En JDBC classique, générer ces rapports aurait nécessité des clauses GROUP BY
         * complexes et de nombreux JOIN sur plusieurs tables (users, tickets, roles),
         * avec une itération manuelle sur un ResultSet pour construire la Map de retour.
         *
         * Avec Spring Data JPA, le framework permet de mapper ces résultats de groupe
         * directement en Map ou structures DTO grâce à HQL/JPQL ou aux méthodes
         * dérivées (Query Methods) très intuitives et puissantes.
         */

        long totalTickets = ticketRepository.count();

        long openTickets = ticketRepository.countByStatusIn(
                List.of(TicketStatus.NEW, TicketStatus.ACCEPTED, TicketStatus.IN_PROGRESS)
        );

        LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
        long resolvedToday = ticketRepository.countByStatusAndDate(
                TicketStatus.RESOLVED, startOfDay);

        long criticalSLA = ticketRepository.countByPriority(Priority.CRITICAL);

        List<Object[]> categoryCounts = ticketRepository.countTicketsByCategoryGroup();
        java.util.Map<String, Long> ticketsByCategory = categoryCounts.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> ((Number) row[1]).longValue()
                ));

        long totalUsers = userRepository.count();

        List<Ticket> resolved = ticketRepository.findAllByStatus(TicketStatus.RESOLVED);
        String avgResolutionTime = "N/A";
        if (!resolved.isEmpty()) {
            long totalMs = resolved.stream()
                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMillis())
                .sum();
            long avgMs = totalMs / resolved.size();
            long hours = avgMs / 3_600_000;
            long mins  = (avgMs % 3_600_000) / 60_000;
            avgResolutionTime = hours + "h " + mins + "m";
        }

        return new com.tickethub.dto.response.AdminStatsResponse(
                totalTickets, openTickets, resolvedToday, criticalSLA, ticketsByCategory, totalUsers, avgResolutionTime);
    }

    private User getCurrentUser() {
        Authentication authentication = getCurrentAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
    }

    private Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ForbiddenOperationException("Authentication is required.");
        }
        return authentication;
    }

    private boolean hasAnyAuthority(Authentication authentication, Set<String> allowedAuthorities) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowedAuthorities::contains);
    }

    private Ticket findTicketByIdOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    private TicketResponse toResponse(Ticket ticket) {
        User author = ticket.getAuthor();
        String authorName = buildAuthorName(author);
        String assigneeName = ticket.getAssignedTechnician() == null
                ? null
                : buildAuthorName(ticket.getAssignedTechnician());

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getSlaDeadline(),
                ticket.getSolution(),
                authorName,
                assigneeName);
    }

    private String buildAuthorName(User author) {
        String firstName = author.getPrenom() == null ? "" : author.getPrenom().trim();
        String lastName = author.getNom() == null ? "" : author.getNom().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? author.getEmail() : fullName;
    }
}
