package com.transport.controller;

import com.transport.entity.User;
import com.transport.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    @Transactional
    public String dashboard(Model model,
                            Authentication authentication,
                            @RequestParam(value = "loginSuccess", required = false) Boolean loginSuccess,
                            HttpServletRequest request) {
        System.out.println("=== DASHBOARD ACCESS ===");

        String username = authentication.getName();
        System.out.println("Dashboard accessed by: " + username);

        Boolean isLoginSuccess = loginSuccess != null && loginSuccess;
        System.out.println("Login success parameter: " + isLoginSuccess);

        User user = userService.findByUsername(username);

        if (user != null) {
            model.addAttribute("currentUser", user);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("fullName", user.getFirstName() + " " + user.getLastName());
            model.addAttribute("email", user.getEmail());

            model.addAttribute("loginSuccess", isLoginSuccess);

            System.out.println("User roles size: " + user.getRoles().size());
            user.getRoles().forEach(role -> {
                System.out.println("Role name: " + role.getName());
                System.out.println("Role toString: " + role.getName().toString());
            });

            String roles = user.getRoles().stream()
                    .map(role -> role.getName().toString().replace("ROLE_", ""))
                    .collect(Collectors.joining(", "));
            model.addAttribute("userRoles", roles);

            Locale currentLocale = LocaleContextHolder.getLocale();
            System.out.println("Current locale: " + currentLocale);

            String translatedRoles = user.getRoles().stream()
                    .map(role -> {
                        String roleName = role.getName().toString();
                        String roleKey = "role." + roleName.replace("ROLE_", "");
                        System.out.println("Looking for translation key: " + roleKey);

                        String translatedRole = messageSource.getMessage(
                                roleKey,
                                null,
                                roleName.replace("ROLE_", ""),
                                currentLocale
                        );
                        System.out.println("Translated role: " + translatedRole);
                        return translatedRole;
                    })
                    .collect(Collectors.joining(", "));

            model.addAttribute("userRolesTranslated", translatedRoles);

            String rolesRaw = user.getRoles().stream()
                    .map(role -> role.getName().toString())
                    .collect(Collectors.joining(", "));
            model.addAttribute("userRolesRaw", rolesRaw);

            //czy użytkownik jest adminem
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().toString().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);

            // DEBUG: Wypisz co trafia do modelu
            System.out.println("userRoles: " + roles);
            System.out.println("userRolesTranslated: " + translatedRoles);
            System.out.println("userRolesRaw: " + rolesRaw);
            System.out.println("isAdmin: " + isAdmin);
            System.out.println("loginSuccess: " + isLoginSuccess);
        }

        return "dashboard";
    }
}