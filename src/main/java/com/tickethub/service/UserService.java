package com.tickethub.service;

import com.tickethub.dto.response.TechnicianAvailabilityResponse;
import com.tickethub.dto.response.TechnicianResponse;
import com.tickethub.dto.response.UserSummaryDTO;
import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.model.Role;
import com.tickethub.model.TicketStatus;
import com.tickethub.model.User;
import com.tickethub.repository.TicketRepository;
import com.tickethub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TicketRepository ticketRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Documentation JDBC vs Spring Data JPA :
     *
     * En JDBC classique, pour récupérer les techniciens (utilisateurs ayant le rôle TECHNICIAN),
     * il aurait fallu écrire une requête native complexe avec une jointure explicite,
     * par exemple :
     * SELECT u.* FROM users u INNER JOIN user_roles ur ON u.id = ur.user_id WHERE ur.role = 'ROLE_TECH';
     * Il aurait ensuite fallu mapper manuellement le ResultSet vers l'objet User.
     *
     * Avec Spring Data JPA, tout cela est géré automatiquement.
     * Le repository 'userRepository.findByRole(Role.ROLE_TECH)' (ou une simple @Query JPQL)
     * s'occupe de la jointure derrière les coulisses grâce au mapping ORM (comme @ElementCollection ou les relations ManyToMany).
     * On obtient une collection d'objets Java prêts à l'emploi.
     */
    public List<TechnicianAvailabilityResponse> getTechniciansAvailability() {
        List<User> technicians = userRepository.findByRole(Role.ROLE_TECH);

        return technicians.stream().map(tech -> {
            long count = ticketRepository.countByAssignedTechnicianIdAndStatusIn(tech.getId(),
                    List.of(TicketStatus.ACCEPTED, TicketStatus.IN_PROGRESS));

            String fullName = java.util.stream.Stream.of(tech.getPrenom(), tech.getNom())
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(" "));
            if (fullName.isBlank()) {
                fullName = tech.getEmail();
            }

            return new TechnicianAvailabilityResponse(
                tech.getId(),
                tech.getNom(),
                tech.getPrenom(),
                tech.getEmail(),
                fullName,
                count
            );
        }).collect(Collectors.toList());
    }

    /**
     * Documentation Pédagogique (J2EE vs Spring Security) :
     *
     * En J2EE classique, tu aurais dû gérer manuellement les états de compte dans
     * la session (HttpSession) ou via des filtres Servlet complexes pour vérifier
     * à chaque requête si l'utilisateur est approuvé.
     *
     * Alors que Spring Security intègre nativement la gestion du statut 'enabled'
     * dans l'interface UserDetails. Si l'attribut boolean 'enabled' est false,
     * Spring Security bloquera la génération du JWT ou l'authentification avec un
     * DisabledException de manière totalement transparente.
     */
    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void createUserByAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already taken.");
        }

        User user = new User();
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setTel(request.getTel());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Admin creates a default tech or client. Assuming Tech for default, but can be improved.
        // Let's assign ROLE_TECH by default or adjust as needed. We'll set ROLE_TECH for now.
        user.setRoles(Set.of(Role.ROLE_TECH));
        user.setEnabled(true); // Account directly approved

        userRepository.save(user);
    }

    public List<UserSummaryDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserSummaryDTO)
                .collect(Collectors.toList());
    }

    public List<UserSummaryDTO> getPendingUsers() {
        return userRepository.findAllByEnabledFalse().stream()
                .map(this::toUserSummaryDTO)
                .collect(Collectors.toList());
    }

    private UserSummaryDTO toUserSummaryDTO(User user) {
        String roleStr = user.getRoles().stream()
                .map(Role::name)
                .sorted((r1, r2) -> {
                    int w1 = r1.equals("ROLE_ADMIN") ? 3 : r1.equals("ROLE_TECH") ? 2 : 1;
                    int w2 = r2.equals("ROLE_ADMIN") ? 3 : r2.equals("ROLE_TECH") ? 2 : 1;
                    return Integer.compare(w2, w1);
                })
                .findFirst()
                .orElse(null);

        String fullName = java.util.stream.Stream.of(user.getPrenom(), user.getNom())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        if (fullName.isBlank()) {
            fullName = user.getEmail();
        }

        return new UserSummaryDTO(
                user.getId(),
                user.getEmail(),
                fullName,
                user.getNom(),
                user.getPrenom(),
                user.getTel(),
                roleStr,
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
