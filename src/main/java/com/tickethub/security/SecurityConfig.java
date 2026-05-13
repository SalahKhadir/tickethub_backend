package com.tickethub.security;

import com.tickethub.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
/*
 * COMPARAISON : J2EE CLASSIQUE (SERVLET) vs SPRING SECURITY
 *
 * 1. Gestion des sessions en Servlet (sans Spring) :
 * L'état d'authentification est généralement maintenu au travers de sessions côté serveur (HttpSession).
 * Après vérification des identifiants (BBD), on stocke l'utilisateur en session : request.getSession().setAttribute("user", user).
 * Un cookie JSESSIONID est alors géré manuellement pour suivre l'utilisateur.
 * Dans notre code Spring, on configure expressément une politique STATELESS (sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 * en faveur du JWT, éliminant ce besoin de session en mémoire.
 *
 * 2. Gestion des filtres de sécurité manuels (sans Spring) :
 * Il faudrait créer des classes implémentant 'javax.servlet.Filter' et redéfinir 'doFilter(request, response, chain)'.
 * La logique impliquerait de vérifier :
 * - Si le chemin requiert une authentification.
 * - Si "user" existe en session ou si les headers sont présents.
 * - Les branchements de code "if/else" sans fin pour interdire ou rediriger selon les rôles.
 *
 * 3. Comparaison avec Spring Security (Configuration automatique, filtres internes, encodage) :
 * - Configuration automatique : L'annotation @Configuration combinée au bean SecurityFilterChain permet
 *   de définir la politique de sécurité de manière centralisée, lisible et déclarative (ex: requestMatchers.hasRole).
 * - Filtres internes : Sous le capot, Spring Security orchestre une chaîne de filtres prédéfinis
 *   (comme le UsernamePasswordAuthenticationFilter). Nous n'avons plus qu'à insérer notre propre
 *   logique 'jwtAuthenticationFilter' au bon endroit via '.addFilterBefore(...)'.
 * - Encodage Password : On n'a plus à hacher manuellement ni concevoir un validateur cryptographique. La déclaration
 *   du bean 'PasswordEncoder' avec 'BCryptPasswordEncoder' indique à Spring comment valider les hashs (salt gérés nativement).
 */
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tickets").hasAnyRole("CLIENT", "TECH", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tickets").hasAnyRole("CLIENT", "TECH", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/tickets/*/status").hasAnyRole("TECH", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/tickets/*/assign").hasAnyRole("TECH", "ADMIN")
                        .requestMatchers("/api/tickets/**").hasAnyRole("CLIENT", "TECH", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/subscribe/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
