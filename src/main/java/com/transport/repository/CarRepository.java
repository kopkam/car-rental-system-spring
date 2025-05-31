package com.transport.repository;

import com.transport.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByStatus(Car.Status status);  // ✅ POPRAWIONE: Car.Status zamiast Car.CarStatus

    List<Car> findByBrandIgnoreCase(String brand);

    List<Car> findByModelIgnoreCase(String model);

    List<Car> findByDailyRateBetween(BigDecimal minRate, BigDecimal maxRate);  // ✅ POPRAWIONE: BigDecimal zamiast Double

    List<Car> findByBrandIgnoreCaseAndModelIgnoreCase(String brand, String model);

    Car findByLicensePlate(String licensePlate);

    List<Car> findByYearBetween(Integer startYear, Integer endYear);
}