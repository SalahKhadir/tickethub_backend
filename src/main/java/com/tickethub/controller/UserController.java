package com.tickethub.controller;

import com.tickethub.dto.response.TechnicianResponse;
import com.tickethub.model.Role;
import com.tickethub.model.User;
import com.tickethub.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserRepository userRepository;
    private final com.tickethub.repository.TicketRepository ticketRepository;

    public UserController(UserRepository userRepository, com.tickethub.repository.TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('TECH','ADMIN')")
    public ResponseEntity<List<TechnicianResponse>> getTechnicians() {
        return ResponseEntity.ok(toTechnicianResponses(userRepository.findByRole(Role.ROLE_TECH)));
    }

    @GetMapping("/users/technicians")
    @PreAuthorize("hasAnyRole('TECH','ADMIN')")
    public ResponseEntity<List<TechnicianResponse>> getTechniciansAlias() {
        return getTechnicians();
    }

    @GetMapping("/users/technicians/availability")
    @PreAuthorize("hasAnyRole('TECH','ADMIN')")
    public ResponseEntity<List<com.tickethub.dto.response.TechnicianAvailabilityResponse>> getTechniciansAvailability() {
        List<User> technicians = userRepository.findByRole(Role.ROLE_TECH);
        List<com.tickethub.dto.response.TechnicianAvailabilityResponse> response = technicians.stream().map(tech -> {
            long count = ticketRepository.countByAssignedTechnicianIdAndStatusIn(tech.getId(),
                    java.util.List.of(com.tickethub.model.TicketStatus.ACCEPTED,
                            com.tickethub.model.TicketStatus.IN_PROGRESS));
            String fullName = java.util.stream.Stream.of(tech.getPrenom(), tech.getNom())
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(" "));
            if (fullName.isBlank()) {
                fullName = tech.getEmail();
            }
            return new com.tickethub.dto.response.TechnicianAvailabilityResponse(tech.getId(), tech.getEmail(),
                    fullName, count);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TechnicianResponse>> getTechniciansAdmin() {
        return getTechnicians();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TechnicianResponse>> getUsers(@RequestParam(required = false) String role) {
        if (role == null || role.isBlank()) {
            return ResponseEntity.ok(toTechnicianResponses(userRepository.findAll()));
        }
        Role parsedRole = parseRole(role);
        return ResponseEntity.ok(toTechnicianResponses(userRepository.findByRole(parsedRole)));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TechnicianResponse>> getUsersAdmin(@RequestParam(required = false) String role) {
        return getUsers(role);
    }

    private List<TechnicianResponse> toTechnicianResponses(List<User> users) {
        return users.stream()
                .map(this::toTechnicianResponse)
                .collect(Collectors.toList());
    }

    private TechnicianResponse toTechnicianResponse(User user) {
        String fullName = Stream.of(user.getPrenom(), user.getNom())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        if (fullName.isBlank()) {
            fullName = user.getEmail();
        }
        return new TechnicianResponse(user.getId(), user.getEmail(), fullName);
    }

    private Role parseRole(String roleValue) {
        String normalized = roleValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("TECHNICIAN") || normalized.equals("TECH")) {
            normalized = "ROLE_TECH";
        } else if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown role: " + roleValue);
        }
    }
}
