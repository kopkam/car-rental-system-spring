package com.transport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)  // ✅ DODANE - potrzebne dla @PreAuthorize
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Publiczne strony
                        .requestMatchers("/", "/register", "/login", "/verify", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // ✅ NOWE - BLOKADA CUSTOMERÓW od zarządzania użytkownikami
                        .requestMatchers("/users/new", "/users/*/delete", "/users/*/toggle-status").hasAnyRole("ADMIN", "MANAGER")  // Admin i Manager mogą dodawać
                        .requestMatchers("/users/assignments/**").hasRole("ADMIN")  // Tylko admin może przypisywać
                        .requestMatchers("/users/**").hasAnyRole("ADMIN", "MANAGER")  // Ogólny dostęp do users dla Admin i Manager

                        // Twoje istniejące reguły
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "MANAGER")

                        // Wszystkie inne wymagają uwierzytelnienia
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                );

        return http.build();
    }
}