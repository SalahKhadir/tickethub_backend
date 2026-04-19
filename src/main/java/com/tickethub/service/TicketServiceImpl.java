package com.tickethub.service;

import com.tickethub.dto.TicketRequest;
import com.tickethub.dto.TicketResponse;
import com.tickethub.exception.ForbiddenOperationException;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.model.Ticket;
import com.tickethub.model.TicketStatus;
import com.tickethub.model.User;
import com.tickethub.repository.TicketRepository;
import com.tickethub.repository.UserRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private static final Set<String> CLIENT_AUTHORITIES = Set.of("ROLE_CLIENT");
    private static final Set<String> STAFF_AUTHORITIES = Set.of("ROLE_TECH", "ROLE_ADMIN");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    public TicketResponse createTicket(TicketRequest request) {
        User currentUser = getCurrentUser();

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .description(request.description())
                .status(TicketStatus.NEW)
                .priority(request.priority())
                .category(request.category())
                .author(currentUser)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        return toResponse(savedTicket);
    }

    @Override
    public List<TicketResponse> getAllTickets() {
        Authentication authentication = getCurrentAuthentication();

        // Un client ne voit que ses tickets, alors que tech/admin voient tout.
        List<Ticket> tickets;
        if (hasAnyAuthority(authentication, STAFF_AUTHORITIES)) {
            tickets = ticketRepository.findAll();
        } else if (hasAnyAuthority(authentication, CLIENT_AUTHORITIES)) {
            User currentUser = getCurrentUser();
            tickets = ticketRepository.findByAuthorId(currentUser.getId());
        } else {
            throw new ForbiddenOperationException("You are not allowed to access tickets.");
        }

        return tickets.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('TECH','ADMIN')")
    public TicketResponse updateTicketStatus(Long id, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);
        return toResponse(updatedTicket);
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
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(allowedAuthorities::contains);
    }

    private TicketResponse toResponse(Ticket ticket) {
        User author = ticket.getAuthor();
        String authorName = buildAuthorName(author);

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                authorName);
    }

    private String buildAuthorName(User author) {
        String firstName = author.getPrenom() == null ? "" : author.getPrenom().trim();
        String lastName = author.getNom() == null ? "" : author.getNom().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? author.getEmail() : fullName;
    }
}

