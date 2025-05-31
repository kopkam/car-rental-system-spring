package com.transport.controller;

import com.transport.entity.User;
import com.transport.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    @Transactional
    public String dashboard(Model model, Authentication authentication) {
        System.out.println("=== DASHBOARD ACCESS ===");

        // Pobierz nazwę użytkownika
        String username = authentication.getName();
        System.out.println("Dashboard accessed by: " + username);

        // Pobierz pełne dane użytkownika z bazy
        User user = userService.findByUsername(username);

        if (user != null) {
            // Dodaj dane użytkownika do modelu
            model.addAttribute("currentUser", user);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("fullName", user.getFirstName() + " " + user.getLastName());
            model.addAttribute("email", user.getEmail());

            // DEBUG: Sprawdź co jest w rolach
            System.out.println("User roles size: " + user.getRoles().size());
            user.getRoles().forEach(role -> {
                System.out.println("Role name: " + role.getName());
                System.out.println("Role toString: " + role.getName().toString());
            });

            // Pobierz role użytkownika - kilka wariantów
            String roles = user.getRoles().stream()
                    .map(role -> role.getName().toString().replace("ROLE_", ""))
                    .collect(Collectors.joining(", "));
            model.addAttribute("userRoles", roles);

            // Alternatywny sposób bez replace
            String rolesRaw = user.getRoles().stream()
                    .map(role -> role.getName().toString())
                    .collect(Collectors.joining(", "));
            model.addAttribute("userRolesRaw", rolesRaw);

            // Sprawdź czy użytkownik jest adminem
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().toString().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);

            // DEBUG: Wypisz co trafia do modelu
            System.out.println("userRoles: " + roles);
            System.out.println("userRolesRaw: " + rolesRaw);
            System.out.println("isAdmin: " + isAdmin);
        }

        return "dashboard";
    }
}