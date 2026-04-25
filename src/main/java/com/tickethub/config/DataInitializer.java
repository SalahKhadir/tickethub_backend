package com.tickethub.config;

import com.tickethub.model.Role;
import com.tickethub.model.User;
import com.tickethub.repository.UserRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    @Profile("local")
    public CommandLineRunner seedDefaultUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("client@tickethub.local")) {
                User clientUser = new User();
                clientUser.setNom("Client");
                clientUser.setPrenom("Demo");
                clientUser.setTel("0000000000");
                clientUser.setEmail("client@tickethub.local");
                clientUser.setPassword(passwordEncoder.encode("password123"));
                clientUser.setRoles(Set.of(Role.ROLE_CLIENT));
                clientUser.setEnabled(true);
                userRepository.save(clientUser);
            }

            if (!userRepository.existsByEmail("tech@tickethub.local")) {
                User techUser = new User();
                techUser.setNom("Tech");
                techUser.setPrenom("Demo");
                techUser.setTel("0612345678");
                techUser.setEmail("tech@tickethub.local");
                techUser.setPassword(passwordEncoder.encode("password123"));
                techUser.setRoles(Set.of(Role.ROLE_TECH));
                techUser.setEnabled(true);
                userRepository.save(techUser);
            }
        };
    }
}
