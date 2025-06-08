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
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model, Authentication authentication) {
        String currentUsername = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        List<User> users = userService.findUsersForCurrentUser(currentUsername);
        String currentUserRole = getCurrentUserRole(authorities);

        model.addAttribute("users", users);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("currentUsername", currentUsername);

        return "users";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ADMIN i MANAGER mogą dodawać użytkowników
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.RoleName.values());
        return "users/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ADMIN i MANAGER mogą tworzyć użytkowników
    public String createUser(@Valid @ModelAttribute User user,
                             BindingResult result,
                             @RequestParam(value = "roleNames", required = false) Set<String> roleNames,
                             Model model,
                             Authentication authentication) {

        if (result.hasErrors()) {
            System.out.println("Form validation errors: " + result.getAllErrors());
            model.addAttribute("roles", Role.RoleName.values());
            return "users/form";
        }

        try {
            // Manager przypisuje customera do siebie
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername);

            System.out.println("Current user from DB: " + (currentUser != null ? currentUser.getUsername() : "null"));
            if (currentUser != null) {
                System.out.println("Current user roles from DB: " + currentUser.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()));
            }

            // Jeśli manager tworzy użytkownika, automatycznie przypisz rolę CUSTOMER
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

        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);

        if (currentUser.isManager() && !userService.canManagerEditUser(currentUsername, id)) {
            return "redirect:/users?error=forbidden";
        }

        String currentUserRole = getCurrentUserRole(authentication.getAuthorities());

        model.addAttribute("user", user);
        model.addAttribute("roles", Role.RoleName.values());
        model.addAttribute("currentUserRole", currentUserRole);

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
                             @RequestParam(value = "managerId", required = false) Long managerId,
                             Model model,
                             Authentication authentication) {

        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return "redirect:/users?error=notfound";
        }

        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);
        String currentUserRole = getCurrentUserRole(authentication.getAuthorities());

        if (currentUser.isManager()) {
            if (!userService.canManagerEditUser(currentUsername, id)) {
                return "redirect:/users?error=forbidden";
            }
            roleNames = null;
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        }

        try {
            user.setId(id);
            userService.updateUser(user, roleNames);

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

            if (existingUser.isCustomer()) {
                List<User> availableManagers = userService.findByRole("ROLE_MANAGER");
                model.addAttribute("availableManagers", availableManagers);
            }

            return "users/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/users?deleted";
    }

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

            if (currentUser.isManager() && !userService.canManagerEditUser(currentUsername, customerId)) {
                return "error:Access denied";
            }

            userService.assignCustomerToManager(customerId, newManagerId);
            return "success:Manager changed successfully";

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    private String getCurrentUserRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "ADMIN";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
            return "MANAGER";
        }
        return "CUSTOMER";
    }

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