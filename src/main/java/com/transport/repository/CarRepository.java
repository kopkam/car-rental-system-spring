package com.transport.repository;

import com.transport.entity.Car;
import com.transport.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByManager(User manager);
    List<Car> findByStatus(Car.Status status);
}