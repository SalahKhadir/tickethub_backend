package com.tickethub.service;

import com.tickethub.dto.response.TechnicianAvailabilityResponse;
import com.tickethub.dto.response.TechnicianResponse;
import com.tickethub.model.Role;
import com.tickethub.model.TicketStatus;
import com.tickethub.model.User;
import com.tickethub.repository.TicketRepository;
import com.tickethub.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public UserService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
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
}

