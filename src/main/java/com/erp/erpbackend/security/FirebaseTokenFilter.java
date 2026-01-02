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
        log.info("🔍 FirebaseTokenFilter: path={}, auth header={}",
                request.getRequestURI(),
                auth != null ? auth.substring(0, Math.min(50, auth.length())) + "..." : "NULL"); // ✅ LOG PATH + HEADER

        if (auth == null || !auth.startsWith("Bearer ")) {
            log.warn("❌ No Bearer token");
            response.setStatus(401);
            response.getWriter().write("Missing Bearer token");
            return; // ✅ STOP HERE - DON'T PROCEED
        }

        String token = auth.substring(7).trim();
        log.info("🔍 Token length={}", token.length()); // ✅ LOG TOKEN LENGTH

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decoded.getUid();
            log.info("✅ Token verified: uid={}", uid); // ✅ SUCCESS LOG

            String role = roleService.getRoleForUid(uid);
            log.info("📋 Role for uid {} = {}", uid, role); // ✅ LOG ROLE

            SimpleGrantedAuthority authority =
                    role == null ? new SimpleGrantedAuthority("ROLE_USER") :
                            new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(uid, null,
                            Collections.singletonList(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ Auth set: uid={}, authorities={}", uid, authority.getAuthority()); // ✅ AUTH SET

            filterChain.doFilter(request, response); // ✅ ONLY HERE
        } catch (Exception e) {
            log.error("❌ Token verification FAILED: {}", e.getMessage(), e); // ✅ DETAILED ERROR
            response.setStatus(401);
            response.getWriter().write("Invalid token: " + e.getMessage());
            return; // ✅ STOP HERE - DON'T PROCEED
        }
    }
}
