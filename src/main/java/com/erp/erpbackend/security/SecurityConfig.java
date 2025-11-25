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
                        // Test endpoint – open
                        .requestMatchers("/api/test/firebase").permitAll()

                        // ==== Students API ====
                        .requestMatchers("/api/students/debug/**").hasRole("ADMIN")
                        .requestMatchers("/api/students/**").authenticated()

                        // ==== Attendance API ====
                        .requestMatchers("/api/attendance/**").authenticated()
                        // (If later you want only ADMIN+TEACHER: use .hasAnyRole("ADMIN","TEACHER"))

                        // ==== Timetable API ====
                        // Everyone logged in (ADMIN, TEACHER, STUDENT) can VIEW timetable
                        .requestMatchers(HttpMethod.GET, "/api/timetable/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        // Only ADMIN + TEACHER can CREATE / UPDATE / DELETE timetable
                        .requestMatchers(HttpMethod.POST, "/api/timetable/**")
                        .hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/timetable/**")
                        .hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/timetable/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        // ==== Everything else ====
                        .anyRequest().permitAll()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
