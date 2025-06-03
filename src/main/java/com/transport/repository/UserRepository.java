package com.transport.repository;

import com.transport.entity.User;
import com.transport.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ✅ ISTNIEJĄCA METODA - znajdź użytkowników po roli
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoles_Name(@Param("roleName") Role.RoleName roleName);

    // ✅ NOWA METODA - znajdź customerów przypisanych do konkretnego managera
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CUSTOMER' AND u.manager.id = :managerId")
    List<User> findCustomersByManagerId(@Param("managerId") Long managerId);

    // ✅ NOWA METODA - policz customerów dla managera
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CUSTOMER' AND u.manager.id = :managerId")
    int countCustomersByManagerId(@Param("managerId") Long managerId);

    // ✅ NOWA METODA - znajdź customerów bez przypisanego managera
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CUSTOMER' AND u.manager IS NULL")
    List<User> findCustomersWithoutManager();

    // ✅ NOWA METODA - znajdź wszystkich managerów z liczbą ich customerów
    @Query("SELECT u, COUNT(c) FROM User u " +
            "LEFT JOIN User c ON c.manager.id = u.id " +
            "JOIN u.roles r WHERE r.name = 'ROLE_MANAGER' " +
            "GROUP BY u.id ORDER BY COUNT(c) ASC")
    List<Object[]> findManagersWithCustomerCount();
}