package com.tickethub.security.jwt;

import com.tickethub.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
/*
 * COMPARAISON : J2EE CLASSIQUE vs SPRING SECURITY (Filtres et Sessions)
 *
 * 1. Gestion des sessions en Servlet (sans Spring) :
 * En pur Servlet, la persistance de l'utilisateur nécessitait l'utilisation de `HttpSession`.
 * Il fallait vérifier manuellement dans chaque requête si la session existait
 * via `request.getSession(false)` et si l'objet User y était attaché.
 *
 * 2. Gestion des filtres de sécurité manuels :
 * Pour protéger des URL sans Spring, il fallait créer un `javax.servlet.Filter` personnalisé
 * et configurer son cycle de vie (web.xml ou @WebFilter). Dans la méthode doFilter(),
 * nous devions extraire les informations (Cookies/Headers), vérifier en BDD manuellement,
 * gérer les blocages `response.sendError(401)` de façon répétitive pour chaque endpoint.
 *
 * 3. Comparaison avec Spring Security :
 * - Filtres internes : En héritant de `OncePerRequestFilter`, Spring garantit une unique
 *   exécution du filtre par requête. Ce filtre intercepte le token, valide l'utilisateur et
 *   l'injecte dans le `SecurityContextHolder`. Ainsi, le reste de l'application a
 *   immédiatement accès à l'utilisateur `Authentication`.
 * - Configuration & Password : Le cryptage n'est pas fait ici à la main. La comparaison
 *   intervient via `AuthenticationManager` et `BCryptPasswordEncoder` (déclarés dans SecurityConfig),
 *   automatisant la vérification salt + hash sécurisée, sans exposer les mots de passe.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        return null;
    }
}
