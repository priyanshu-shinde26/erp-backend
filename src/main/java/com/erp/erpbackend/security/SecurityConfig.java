package com.erp.erpbackend.security;

import com.erp.erpbackend.service.RoleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
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
   // @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public SecurityFilterChain filterChain(HttpSecurity http, FirebaseTokenFilter firebaseTokenFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/test/firebase").permitAll()
                        .requestMatchers("/api/students/**").authenticated()
                        .requestMatchers("/api/attendance/**").authenticated()
                        .requestMatchers("/api/students/debug/**").hasRole("ADMIN")
                        //.requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "FACULTY")
                        .anyRequest().permitAll()

                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
