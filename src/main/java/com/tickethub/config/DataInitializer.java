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
            if (userRepository.existsByEmail("client@tickethub.local")) {
                return;
            }

            User user = new User();
            user.setNom("Client");
            user.setPrenom("Demo");
            user.setTel("0000000000");
            user.setEmail("client@tickethub.local");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRoles(Set.of(Role.ROLE_CLIENT));
            userRepository.save(user);
        };
    }
}
