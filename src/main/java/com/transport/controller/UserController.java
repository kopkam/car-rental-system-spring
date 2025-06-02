package com.transport.controller;

import com.transport.entity.Role;
import com.transport.entity.User;
import com.transport.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users"; // Zmienione z "users/list" na "users"
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.RoleName.values());
        return "users/form"; // lub "user-form" jeśli masz taki plik
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute User user,
                             BindingResult result,
                             @RequestParam(value = "roleNames", required = false) Set<String> roleNames,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.RoleName.values());
            return "users/form";
        }

        try {
            userService.createUser(user, roleNames);
            return "redirect:/users?success";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.RoleName.values());
            return "users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/users?error=notfound";
        }

        model.addAttribute("user", user);
        model.addAttribute("roles", Role.RoleName.values());
        return "users/form";
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,  // USUNĄŁEM @Valid
                             BindingResult result,
                             @RequestParam(value = "roleNames", required = false) Set<String> roleNames,
                             Model model) {

        // Pobierz istniejącego użytkownika z bazy danych
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return "redirect:/users?error=notfound";
        }

        // Jeśli password jest puste, zachowaj stare hasło
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        }

        // Teraz możesz walidować ręcznie lub użyć @Valid
        // Ale password już nie będzie puste

        try {
            user.setId(id);
            userService.updateUser(user, roleNames);
            return "redirect:/users?updated";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.RoleName.values());
            return "users/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/users?deleted";
    }
}