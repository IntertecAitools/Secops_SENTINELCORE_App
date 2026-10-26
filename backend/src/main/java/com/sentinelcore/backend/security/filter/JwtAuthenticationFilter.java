package com.sentinelcore.backend.security.filter;

import com.sentinelcore.backend.security.jwt.JwtService;
import com.sentinelcore.backend.security.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            // Validates signature, expiry, issuer and audience
            Claims claims = jwtService.parseAndValidate(token);
            String username = claims.getSubject();

            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // Authorities are reloaded from the database, never trusted from the token
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()
                        && jwtService.isTokenValid(token, userDetails.getUsername())) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // Continue unauthenticated; do not log token contents
            logger.warn("JWT validation failed: " + e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }
}
