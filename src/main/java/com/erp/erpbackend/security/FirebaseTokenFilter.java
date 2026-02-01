package com.erp.erpbackend.security;

import com.erp.erpbackend.service.RoleService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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
import java.util.List;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final RoleService roleService;

    public FirebaseTokenFilter(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Logging the request for debugging
        log.info("🔍 FirebaseTokenFilter: path={}, auth header present={}",
                request.getRequestURI(),
                authHeader != null);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("❌ No Bearer token found");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Bearer token");
            return;
        }

        String token = authHeader.substring(7); // remove "Bearer "

        try {
            // 🔥 This is what tells Spring: "This Firebase token is valid"
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decodedToken.getUid();
            String role = roleService.getRoleForUid(uid); // ADMIN / TEACHER / STUDENT

            // Ensure role is not null and assign it to authorities
            String assignedRole = (role != null) ? role : "USER";

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            uid,
                            null,
                            List.of(new SimpleGrantedAuthority(assignedRole))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("uid", uid);

            log.info("✅ Auth set: uid={}, role={}", uid, assignedRole);

            // Proceed to the next filter
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("❌ Token verification FAILED: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token: " + e.getMessage());
        }
    }
}