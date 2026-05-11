package com.tickethub.service.impl;

import com.tickethub.dto.request.TicketRequest;
import com.tickethub.dto.request.TicketUpdateRequest;
import com.tickethub.dto.response.TicketResponse;
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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return toResponse(savedTicket);
    }

    @Override
    public Page<TicketResponse> getAllTickets(
            Pageable pageable,
            TicketStatus status,
            Priority priority,
            TicketCategory category) {
        Authentication authentication = getCurrentAuthentication();

        boolean isAdmin = hasAnyAuthority(authentication, ADMIN_AUTHORITIES);
        boolean isTech = hasAnyAuthority(authentication, TECH_AUTHORITIES);

        Page<Ticket> tickets;
        if (isTech && !isAdmin) {
            User currentUser = getCurrentUser();
            tickets = ticketRepository.findByAssignedTechnicianIdWithFilters(
                    currentUser.getId(),
                    status,
                    priority,
                    category,
                    pageable);
        } else if (isAdmin) {
            tickets = ticketRepository.findAllWithFilters(status, priority, category, pageable);
        } else if (hasAnyAuthority(authentication, CLIENT_AUTHORITIES)) {
            User currentUser = getCurrentUser();
            tickets = ticketRepository.findByAuthorIdWithFilters(
                    currentUser.getId(),
                    status,
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
        return toResponse(updatedTicket);
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
        return toResponse(updatedTicket);
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
