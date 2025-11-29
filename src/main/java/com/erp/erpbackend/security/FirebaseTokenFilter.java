package com.erp.erpbackend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.erp.erpbackend.service.RoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class FirebaseTokenFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final RoleService roleService;

    public FirebaseTokenFilter(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            try {
                FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
                String uid = decoded.getUid();

                // Minimal authority: read role if you want to attach roles (fast path uses cache)
                String role = roleService.getRoleForUid(uid);
                SimpleGrantedAuthority authority =
                        role == null ? new SimpleGrantedAuthority("ROLE_USER") :
                                new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(uid, null,
                                Collections.singletonList(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.warn("Firebase token verification failed", e);
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}