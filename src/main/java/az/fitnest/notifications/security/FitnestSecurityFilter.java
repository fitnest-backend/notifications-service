package az.fitnest.notifications.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FitnestSecurityFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    
    private static final java.util.Set<String> SKIP_FILTER_PATH_PREFIXES = java.util.Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/webjars",
            "/error"
    );

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_FILTER_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

     @Override
     protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String gatewayFlag = request.getHeader("X-From-Gateway");
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String requestId = request.getHeader("X-Request-Id");
        String caller = request.getHeader("X-Service-Name");

        // Prefer Pattern A headers if from Gateway
        if ("1".equals(gatewayFlag) && userIdHeader != null && !userIdHeader.isBlank()) {
            authenticateViaPatternA(request, userIdHeader, requestId, caller);
        } else {
            // Legacy/Direct JWT support
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authenticateViaJwt(authHeader.substring(7));
            }
            
            if (request.getRequestURI().startsWith("/api/v1/internal")) {
                authenticateViaInternalHeaders(request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateViaPatternA(HttpServletRequest request, String userIdStr, String requestId, String caller) {
        try {
            Long userId = Long.parseLong(userIdStr);
            String scopes = request.getHeader("X-Scopes");
            String email = request.getHeader(USER_EMAIL_HEADER);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL"));

            if (scopes != null && !scopes.isBlank()) {
                authorities.addAll(Arrays.stream(scopes.split(" "))
                        .map(role -> role.trim().toUpperCase())
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            auth.setDetails("PatternA:" + caller + ":" + requestId + (email != null ? ":" + email : ""));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
        }
    }

    private void authenticateViaInternalHeaders(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        
        if (userIdStr != null && !userIdStr.isBlank()) {
            authenticateGatewayUser(request);
        } else {
            authenticateInternalService();
        }
    }

    private void authenticateViaJwt(String token) {
        try {
            Long userId = jwtService.parseUserId(token);
            List<String> roles = jwtService.parseRoles(token);
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (roles.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            } else {
                authorities = roles.stream()
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user {} via JWT with authorities {}", userId, authorities);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
    }

    private void authenticateInternalService() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "INTERNAL_SERVICE", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateGatewayUser(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String rolesStr = request.getHeader(USER_ROLES_HEADER);

        try {
            Long userId = Long.parseLong(userIdStr);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL"));

            if (rolesStr != null && !rolesStr.isBlank()) {
                authorities.addAll(Arrays.stream(rolesStr.split(","))
                        .map(role -> role.trim().toUpperCase())
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            auth.setDetails(email);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated internal request for user {} with ROLE_INTERNAL", userId);
        } catch (NumberFormatException e) {
        }
    }
}
