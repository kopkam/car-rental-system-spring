package com.transport.service;

import com.transport.entity.Car;
import com.transport.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {

    @Autowired
    private CarRepository carRepository;

    public List<Car> getAllCars() {
        try {
            List<Car> cars = carRepository.findAll();
            System.out.println("Found " + cars.size() + " cars in database");
            return cars;
        } catch (Exception e) {
            System.err.println("Error fetching cars: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // ✅ PROSTA METODA SAVE - używana przez BookingController
    public Car save(Car car) {
        return carRepository.save(car);
    }

    // ✅ METODA SAVE Z WALIDACJĄ - używana przy tworzeniu/edycji aut
    @Transactional
    public Car saveCar(Car car) {
        try {
            System.out.println("=== SAVING CAR ===");
            System.out.println("Car: " + car);

            // Check for duplicate license plate
            if (car.getId() == null) { // New car
                Car existingCar = carRepository.findByLicensePlate(car.getLicensePlate());
                if (existingCar != null) {
                    throw new RuntimeException("Car with license plate " + car.getLicensePlate() + " already exists");
                }
                // Set createdAt for new cars
                car.setCreatedAt(LocalDateTime.now());
            } else { // Updating existing car
                Car existingCar = carRepository.findByLicensePlate(car.getLicensePlate());
                if (existingCar != null && !existingCar.getId().equals(car.getId())) {
                    throw new RuntimeException("Another car with license plate " + car.getLicensePlate() + " already exists");
                }
            }

            Car savedCar = carRepository.save(car);
            System.out.println("Car saved with ID: " + savedCar.getId());
            return savedCar;

        } catch (Exception e) {
            System.err.println("Error saving car: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<Car> getAvailableCars() {
        return carRepository.findByStatus(Car.Status.AVAILABLE);
    }

    public Car getCarById(Long id) {
        try {
            Optional<Car> car = carRepository.findById(id);
            if (car.isPresent()) {
                System.out.println("Found car: " + car.get());
                return car.get();
            } else {
                System.out.println("Car not found with ID: " + id);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error fetching car by ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public void deleteCar(Long id) {
        try {
            System.out.println("=== DELETING CAR ===");
            System.out.println("Car ID: " + id);

            Car car = getCarById(id);
            if (car == null) {
                throw new RuntimeException("Car not found with ID: " + id);
            }

            // Check if car has active bookings
            if (car.getBookings() != null && !car.getBookings().isEmpty()) {
                long activeBookings = car.getBookings().stream()
                        .filter(booking -> booking.getStatus() != null &&
                                !booking.getStatus().toString().equals("CANCELLED"))
                        .count();

                if (activeBookings > 0) {
                    throw new RuntimeException("Cannot delete car with active bookings");
                }
            }

            carRepository.deleteById(id);
            System.out.println("Car deleted successfully");

        } catch (Exception e) {
            System.err.println("Error deleting car: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<Car> findByBrand(String brand) {
        return carRepository.findByBrandIgnoreCase(brand);
    }

    public List<Car> findByModel(String model) {
        return carRepository.findByModelIgnoreCase(model);
    }

    public List<Car> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return carRepository.findByDailyRateBetween(minPrice, maxPrice);
    }

    public List<Car> findByBrandAndModel(String brand, String model) {
        return carRepository.findByBrandIgnoreCaseAndModelIgnoreCase(brand, model);
    }

    public Car findByLicensePlate(String licensePlate) {
        return carRepository.findByLicensePlate(licensePlate);
    }

    public List<Car> findByYearRange(Integer startYear, Integer endYear) {
        return carRepository.findByYearBetween(startYear, endYear);
    }

    // Dodatkowe metody dla statusów
    public List<Car> getCarsByStatus(Car.Status status) {
        return carRepository.findByStatus(status);
    }

    public Car rentCar(Long id) {
        Car car = getCarById(id);
        if (car != null && car.getStatus() == Car.Status.AVAILABLE) {
            car.setStatus(Car.Status.RENTED);
            return save(car);
        }
        return null;
    }

    public Car returnCar(Long id) {
        Car car = getCarById(id);
        if (car != null && car.getStatus() == Car.Status.RENTED) {
            car.setStatus(Car.Status.AVAILABLE);
            return save(car);
        }
        return null;
    }
}