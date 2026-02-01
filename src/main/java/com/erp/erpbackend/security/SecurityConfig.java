package com.erp.erpbackend.security;

import com.erp.erpbackend.service.RoleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter(RoleService roleService) {
        return new FirebaseTokenFilter(roleService);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            FirebaseTokenFilter firebaseTokenFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // 🔥 SESSION MANAGEMENT: STATELESS (MANDATORY FOR JWT/FIREBASE)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ================= ACADEMIC MODULE =================
                        .requestMatchers("/api/academic/tests").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/academic/**").authenticated()
                        // ================= PUBLIC / TEST =================
                        .requestMatchers("/api/test/**").permitAll()

                        // ================= ASSIGNMENTS =================
                        .requestMatchers("/api/assignments/**").authenticated()

                        // ================= STUDENTS =================
                        .requestMatchers("/api/students/debug/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/students/**").authenticated()

                        // ================= ATTENDANCE =================
                        .requestMatchers("/api/attendance/**").authenticated()

                        // ================= TIMETABLE =================
                        .requestMatchers(HttpMethod.GET, "/api/timetable/**")
                        .hasAnyAuthority("TEACHER", "ADMIN", "STUDENT")

                        .requestMatchers(HttpMethod.POST, "/api/timetable/**")
                        .hasAnyAuthority("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/timetable/**")
                        .hasAnyAuthority("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/timetable/**")
                        .hasAnyAuthority("TEACHER", "ADMIN")

                        // ================= FALLBACK =================
                        .anyRequest().permitAll()
                );

        // 🔥 VERY IMPORTANT: REGISTER THE FILTER
        http.addFilterBefore(
                firebaseTokenFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}