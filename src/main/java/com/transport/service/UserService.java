package com.transport.service;

import com.transport.entity.Role;
import com.transport.entity.User;
import com.transport.entity.VerificationToken;
import com.transport.repository.RoleRepository;
import com.transport.repository.UserRepository;
import com.transport.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Customer role not found"));
        user.setRoles(Set.of(customerRole));

        // ✅ NOWA LOGIKA - Automatyczne przypisanie do managera (round-robin)
        User assignedManager = findManagerWithLeastCustomers();
        if (assignedManager != null) {
            user.setManager(assignedManager);
            System.out.println("Assigned customer " + user.getUsername() + " to manager " + assignedManager.getUsername());
        }

        User savedUser = userRepository.save(user);

        // Create verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, savedUser);
        tokenRepository.save(verificationToken);

        // Send confirmation email
        emailService.sendVerificationEmail(savedUser, token);

        return savedUser;
    }

    // ✅ NOWA METODA - znajdź managera z najmniejszą liczbą customerów
    public User findManagerWithLeastCustomers() {
        List<User> managers = findByRole("ROLE_MANAGER");
        if (managers.isEmpty()) {
            System.out.println("No managers found in system");
            return null;
        }

        User managerWithLeastCustomers = managers.get(0);
        int minCustomers = countCustomersForManager(managerWithLeastCustomers.getId());

        for (User manager : managers) {
            int customerCount = countCustomersForManager(manager.getId());
            if (customerCount < minCustomers) {
                minCustomers = customerCount;
                managerWithLeastCustomers = manager;
            }
        }

        System.out.println("Manager " + managerWithLeastCustomers.getUsername() + " has " + minCustomers + " customers");
        return managerWithLeastCustomers;
    }

    // ✅ NOWA METODA - policz customerów dla managera
    public int countCustomersForManager(Long managerId) {
        return userRepository.countCustomersByManagerId(managerId);
    }

    public boolean verifyUser(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken != null && !verificationToken.isExpired()) {
            User user = verificationToken.getUser();
            user.setEnabled(true);
            userRepository.save(user);
            tokenRepository.delete(verificationToken);
            return true;  // ✅ Zwróć true jeśli weryfikacja się powiodła
        }
        return false;  // ✅ Zwróć false jeśli token nieprawidłowy lub wygasł
    }

    public User findByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.orElse(null);
    }

    public User findByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    // ✅ ZAKTUALIZOWANA METODA - znajdź użytkowników widocznych dla danego użytkownika
    public List<User> findUsersForCurrentUser(String currentUsername) {
        User currentUser = findByUsername(currentUsername);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        if (currentUser.isAdmin()) {
            // Admin widzi wszystkich użytkowników
            return findAll();
        } else if (currentUser.isManager()) {
            // Manager widzi tylko swoich customerów
            return findCustomersByManager(currentUser.getId());
        } else {
            // Customer nie widzi żadnych użytkowników
            return new ArrayList<>();
        }
    }

    // ✅ NOWA METODA - znajdź customerów przypisanych do managera
    public List<User> findCustomersByManager(Long managerId) {
        return userRepository.findCustomersByManagerId(managerId);
    }

    // ✅ NOWA METODA - sprawdź czy manager może edytować użytkownika
    public boolean canManagerEditUser(String managerUsername, Long userId) {
        User manager = findByUsername(managerUsername);
        if (manager == null || !manager.isManager()) {
            return false;
        }

        User userToEdit = findById(userId);
        if (userToEdit == null) {
            return false;
        }

        // Manager może edytować tylko swoich customerów
        return userToEdit.isCustomer() &&
                userToEdit.getManager() != null &&
                userToEdit.getManager().getId().equals(manager.getId());
    }

    // ✅ ZAKTUALIZOWANA METODA - znajdź użytkowników po roli
    public List<User> findByRole(String roleName) {
        try {
            Role.RoleName roleNameEnum = Role.RoleName.valueOf(roleName);
            return userRepository.findByRoles_Name(roleNameEnum);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid role name: " + roleName);
            return new ArrayList<>();
        }
    }

    public User createUser(User user, Set<String> roleNames) {
        return createUser(user, roleNames, null);  // Dla kompatybilności wstecznej
    }

    // ✅ NOWA PRZECIĄŻONA METODA z logiką przypisywania managera
    public User createUser(User user, Set<String> roleNames, User createdBy) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set roles
        Set<Role> roles = new HashSet<>();
        if (roleNames != null && !roleNames.isEmpty()) {
            for (String roleName : roleNames) {
                Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);
                Role role = roleRepository.findByName(roleEnum)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                roles.add(role);
            }
        } else {
            // Default to CUSTOMER role
            Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Customer role not found"));
            roles.add(customerRole);
        }
        user.setRoles(roles);

        // ✅ NOWA LOGIKA PRZYPISYWANIA MANAGERA
        boolean isCustomer = roleNames != null && roleNames.contains("ROLE_CUSTOMER");
        if (isCustomer) {
            if (createdBy != null && createdBy.isManager()) {
                // Jeśli manager tworzy customera, przypisz go do siebie
                user.setManager(createdBy);
                System.out.println("Manager " + createdBy.getUsername() + " created customer " + user.getUsername() + " - assigned to self");
            } else {
                // Jeśli admin tworzy customera, użyj round-robin
                User assignedManager = findManagerWithLeastCustomers();
                if (assignedManager != null) {
                    user.setManager(assignedManager);
                    System.out.println("Admin created customer " + user.getUsername() + " - assigned to manager " + assignedManager.getUsername());
                }
            }
        }

        // Enable user by default for admin-created users
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public User updateUser(User user, Set<String> roleNames) {
        User existingUser = findById(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

        // Update basic fields
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());

        // Update password only if provided
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Update roles
        if (roleNames != null) {
            Set<Role> roles = roleNames.stream()
                    .map(roleName -> {
                        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);
                        return roleRepository.findByName(roleEnum)
                                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                    })
                    .collect(Collectors.toSet());
            existingUser.setRoles(roles);

            // ✅ NOWA LOGIKA - Jeśli zmieniamy rolę na customer, przypisz managera
            boolean isCustomer = roleNames.contains("ROLE_CUSTOMER");
            if (isCustomer && existingUser.getManager() == null) {
                User assignedManager = findManagerWithLeastCustomers();
                if (assignedManager != null) {
                    existingUser.setManager(assignedManager);
                }
            }
        }

        return userRepository.save(existingUser);
    }

    public void toggleUserStatus(Long id) {
        User user = findById(id);
        if (user != null) {
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
        }
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    // ✅ POMOCNICZA METODA - sprawdza czy user istnieje po username
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // ✅ POMOCNICZA METODA - sprawdza czy user istnieje po email
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ✅ NOWA METODA - przypisz customera do managera ręcznie (dla admina)
    public void assignCustomerToManager(Long customerId, Long managerId) {
        User customer = findById(customerId);
        User manager = findById(managerId);

        if (customer != null && manager != null &&
                customer.isCustomer() && manager.isManager()) {
            customer.setManager(manager);
            userRepository.save(customer);
        }
    }

    // ✅ NOWA METODA - usuń przypisanie customera od managera
    public void removeCustomerFromManager(Long customerId) {
        User customer = findById(customerId);
        if (customer != null && customer.isCustomer()) {
            customer.setManager(null);
            userRepository.save(customer);
        }
    }

    // ✅ NOWA METODA - znajdź customerów bez managera
    public List<User> findCustomersWithoutManager() {
        return userRepository.findCustomersWithoutManager();
    }
}