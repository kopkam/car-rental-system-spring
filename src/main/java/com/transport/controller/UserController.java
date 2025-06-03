package com.transport.controller;

import com.transport.entity.Role;
import com.transport.entity.User;
import com.transport.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // Customer nie ma dostępu wcale
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model, Authentication authentication) {
        String currentUsername = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // ✅ NOWA LOGIKA - różne listy dla różnych ról
        List<User> users = userService.findUsersForCurrentUser(currentUsername);
        String currentUserRole = getCurrentUserRole(authorities);

        model.addAttribute("users", users);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("currentUsername", currentUsername);

        return "users"; // Zmienione z "users/list" na "users"
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ✅ ADMIN i MANAGER mogą dodawać użytkowników
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.RoleName.values());
        return "users/form"; // lub "user-form" jeśli masz taki plik
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ✅ ADMIN i MANAGER mogą tworzyć użytkowników
    public String createUser(@Valid @ModelAttribute User user,
                             BindingResult result,
                             @RequestParam(value = "roleNames", required = false) Set<String> roleNames,
                             Model model,
                             Authentication authentication) {  // ✅ DODANE - potrzebne do identyfikacji aktualnego użytkownika

        // ✅ DEBUG LOGGING
        System.out.println("=== CONTROLLER CREATE USER DEBUG ===");
        System.out.println("User: " + user.getUsername());
        System.out.println("RoleNames from form: " + roleNames);
        System.out.println("Current user: " + authentication.getName());
        System.out.println("User authorities: " + authentication.getAuthorities());

        if (result.hasErrors()) {
            System.out.println("Form validation errors: " + result.getAllErrors());
            model.addAttribute("roles", Role.RoleName.values());
            return "users/form";
        }

        try {
            // ✅ NOWA LOGIKA - Manager przypisuje customera do siebie
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername);

            System.out.println("Current user from DB: " + (currentUser != null ? currentUser.getUsername() : "null"));
            if (currentUser != null) {
                System.out.println("Current user roles from DB: " + currentUser.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()));
            }

            // ✅ POPRAWKA - Jeśli manager tworzy użytkownika, automatycznie przypisz rolę CUSTOMER
            if (currentUser != null && currentUser.isManager() && (roleNames == null || roleNames.isEmpty())) {
                roleNames = Set.of("ROLE_CUSTOMER");
                System.out.println("Manager creating user - automatically assigned ROLE_CUSTOMER");
            }

            System.out.println("Final roleNames: " + roleNames);

            userService.createUser(user, roleNames, currentUser);  // Przekazujemy aktualnego użytkownika
            return "redirect:/users?success";
        } catch (Exception e) {
            System.out.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.RoleName.values());
            return "users/form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @userService.canManagerEditUser(authentication.name, #id))")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/users?error=notfound";
        }

        // ✅ DODATKOWA KONTROLA - sprawdź uprawnienia
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);

        if (currentUser.isManager() && !userService.canManagerEditUser(currentUsername, id)) {
            return "redirect:/users?error=forbidden";
        }

        String currentUserRole = getCurrentUserRole(authentication.getAuthorities());

        model.addAttribute("user", user);
        model.addAttribute("roles", Role.RoleName.values());
        model.addAttribute("currentUserRole", currentUserRole);

        // ✅ NOWA FUNKCJONALNOŚĆ - lista managerów do wyboru (dla admina i managera)
        if (user.isCustomer()) {
            List<User> availableManagers = userService.findByRole("ROLE_MANAGER");
            model.addAttribute("availableManagers", availableManagers);
        }

        return "users/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @userService.canManagerEditUser(authentication.name, #id))")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             BindingResult result,
                             @RequestParam(value = "roleNames", required = false) Set<String> roleNames,
                             @RequestParam(value = "managerId", required = false) Long managerId, // ✅ NOWY PARAMETR
                             Model model,
                             Authentication authentication) {

        // Pobierz istniejącego użytkownika z bazy danych
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return "redirect:/users?error=notfound";
        }

        // ✅ DODATKOWA KONTROLA dla managerów
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);
        String currentUserRole = getCurrentUserRole(authentication.getAuthorities());

        if (currentUser.isManager()) {
            if (!userService.canManagerEditUser(currentUsername, id)) {
                return "redirect:/users?error=forbidden";
            }
            // Manager może zmieniać tylko podstawowe dane i przypisanie managera, nie role
            roleNames = null;
        }

        // Jeśli password jest puste, zachowaj stare hasło
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        }

        try {
            user.setId(id);
            userService.updateUser(user, roleNames);

            // ✅ NOWA LOGIKA - zmiana managera dla customera
            if (existingUser.isCustomer() && managerId != null && managerId > 0) {
                // Admin może zmieniać managera dla każdego customera
                // Manager może zmieniać managera tylko dla swoich customerów
                if (currentUser.isAdmin() ||
                        (currentUser.isManager() && userService.canManagerEditUser(currentUsername, id))) {

                    userService.assignCustomerToManager(id, managerId);
                }
            }

            return "redirect:/users?updated";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.RoleName.values());
            model.addAttribute("currentUserRole", currentUserRole);

            // Ponownie dodaj listę managerów w przypadku błędu
            if (existingUser.isCustomer()) {
                List<User> availableManagers = userService.findByRole("ROLE_MANAGER");
                model.addAttribute("availableManagers", availableManagers);
            }

            return "users/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")  // Tylko admin może zmieniać status
    public String toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")  // Tylko admin może usuwać użytkowników
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/users?deleted";
    }

    // ✅ NOWA METODA - panel przypisywania customerów do managerów (tylko dla admina)
    @GetMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAssignments(Model model) {
        List<User> managers = userService.findByRole("ROLE_MANAGER");
        List<User> customers = userService.findByRole("ROLE_CUSTOMER");
        List<User> unassignedCustomers = userService.findCustomersWithoutManager();

        model.addAttribute("managers", managers);
        model.addAttribute("customers", customers);
        model.addAttribute("unassignedCustomers", unassignedCustomers);

        return "users/assignments";
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignCustomerToManager(@RequestParam Long customerId,
                                          @RequestParam Long managerId) {
        userService.assignCustomerToManager(customerId, managerId);
        return "redirect:/users/assignments?assigned";
    }

    @PostMapping("/{customerId}/remove-manager")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeCustomerFromManager(@PathVariable Long customerId) {
        userService.removeCustomerFromManager(customerId);
        return "redirect:/users/assignments?removed";
    }

    // ✅ NOWA METODA - szybka zmiana managera dla customera (AJAX endpoint)
    @PostMapping("/{customerId}/change-manager")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @userService.canManagerEditUser(authentication.name, #customerId))")
    @ResponseBody
    public String changeCustomerManager(@PathVariable Long customerId,
                                        @RequestParam Long newManagerId,
                                        Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername);
            User customer = userService.findById(customerId);

            if (customer == null || !customer.isCustomer()) {
                return "error:Customer not found";
            }

            // Sprawdź uprawnienia
            if (currentUser.isManager() && !userService.canManagerEditUser(currentUsername, customerId)) {
                return "error:Access denied";
            }

            userService.assignCustomerToManager(customerId, newManagerId);
            return "success:Manager changed successfully";

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    // ✅ POMOCNICZA METODA - określ rolę użytkownika
    private String getCurrentUserRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "ADMIN";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
            return "MANAGER";
        }
        return "CUSTOMER";
    }

    // ✅ NOWA METODA - statystyki dla managera
    @GetMapping("/my-customers")
    @PreAuthorize("hasRole('MANAGER')")
    public String myCustomers(Model model, Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentManager = userService.findByUsername(currentUsername);

        if (currentManager != null) {
            List<User> myCustomers = userService.findCustomersByManager(currentManager.getId());
            model.addAttribute("customers", myCustomers);
            model.addAttribute("manager", currentManager);
        }

        return "users/my-customers";
    }
}