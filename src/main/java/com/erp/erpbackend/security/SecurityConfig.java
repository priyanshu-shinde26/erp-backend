package com.erp.erpbackend.security;

import com.erp.erpbackend.service.RoleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter(RoleService roleService) {
        return new FirebaseTokenFilter(roleService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           FirebaseTokenFilter firebaseTokenFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ---- Public / test endpoints ----
                        .requestMatchers("/api/test/**").permitAll()

                        // ---- Assignment module ----
                        .requestMatchers("/api/assignments/**").authenticated()

                        // ---- Existing APIs ----
                        .requestMatchers("/api/students/debug/**").hasRole("ADMIN")
                        .requestMatchers("/api/students/**").authenticated()
                        .requestMatchers("/api/attendance/**").authenticated()

                        // ================= TIMETABLE ✅ COMPLETE =================
                        .requestMatchers(HttpMethod.GET, "/api/timetable/**")
                        .hasAnyRole("TEACHER", "ADMIN", "STUDENT")  // ✅ GET view for all

                        .requestMatchers(HttpMethod.POST, "/api/timetable/**")
                        .hasAnyRole("TEACHER", "ADMIN")  // ✅ POST create for teacher/admin

                        .requestMatchers(HttpMethod.PUT, "/api/timetable/**")
                        .hasAnyRole("TEACHER", "ADMIN")  // ✅ PUT update

                        .requestMatchers(HttpMethod.DELETE, "/api/timetable/**")
                        .hasAnyRole("TEACHER", "ADMIN")  // ✅ DELETE

                        // ========================================================

                        .anyRequest().permitAll()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
