package com.transport.service;

import com.transport.entity.Car;
import com.transport.entity.User;
import com.transport.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CarService {

    @Autowired
    private CarRepository carRepository;

    @Value("${app.upload.dir:uploads/cars/}")
    private String uploadDir;

    @PostConstruct
    public void initUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
            } else {
                System.out.println("Upload directory exists: " + uploadPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Failed to create upload directory: " + e.getMessage());
        }
    }

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

    public Car save(Car car) {
        return carRepository.save(car);
    }

    @Transactional
    public Car saveCar(Car car, User currentUser, MultipartFile imageFile) {
        System.out.println("=== SAVING CAR ===");
        System.out.println("Car: " + car);
        System.out.println("Current user: " + currentUser.getUsername());

        try {
            if (car.getId() == null) { // tylko dla nowych aut
                Car existingCar = carRepository.findByLicensePlate(car.getLicensePlate());
                if (existingCar != null) {
                    throw new RuntimeException("Car with license plate " + car.getLicensePlate() + " already exists!");
                }
            }

            if (currentUser.hasRole("ROLE_ADMIN") && car.getManager() == null) {
            } else if (currentUser.hasRole("ROLE_MANAGER")) {
                car.setManager(currentUser);
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("Image file: " + imageFile.getOriginalFilename());

                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new RuntimeException("Please upload a valid image file!");
                }

                String fileExtension = getFileExtension(imageFile.getOriginalFilename());
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(uniqueFilename);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                System.out.printf("Image saved to: %s%n", filePath.toAbsolutePath());
                System.out.printf("🖼Saved image: %s%n", uniqueFilename);

                car.setImageUrl("/images/cars/" + uniqueFilename);
            }

            if (car.getCreatedAt() == null) {
                car.setCreatedAt(LocalDateTime.now());
            }

            Car savedCar = carRepository.save(car);
            System.out.println("Car saved with ID: " + savedCar.getId() +
                    (savedCar.getImageUrl() != null ? " (Image: " +
                            savedCar.getImageUrl().substring(savedCar.getImageUrl().lastIndexOf("/") + 1) + ")" : ""));

            return savedCar;
        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            throw new RuntimeException("Failed to save image: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    @Transactional
    public Car saveCar(Car car, User currentUser) {
        return saveCar(car, currentUser, null);
    }

    @Transactional
    public Car saveCar(Car car) {
        return carRepository.save(car);
    }

    private String saveImage(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("File is not an image");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID().toString() + extension;

        Path uploadPath = Paths.get(uploadDir);
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("📸 Image saved to: " + filePath.toAbsolutePath());

        return filename;
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

            if (car.getBookings() != null && !car.getBookings().isEmpty()) {
                long activeBookings = car.getBookings().stream()
                        .filter(booking -> booking.getStatus() != null &&
                                !booking.getStatus().toString().equals("CANCELLED"))
                        .count();

                if (activeBookings > 0) {
                    throw new RuntimeException("Cannot delete car with active bookings");
                }
            }

            if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
                try {
                    String filename = car.getImageUrl().substring(car.getImageUrl().lastIndexOf("/") + 1);
                    Path imagePath = Paths.get(uploadDir).resolve(filename);
                    Files.deleteIfExists(imagePath);
                    System.out.println("🗑️ Deleted image: " + imagePath);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to delete image: " + e.getMessage());
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

    public List<Car> getCarsByStatus(Car.Status status) {
        return carRepository.findByStatus(status);
    }

    public List<Car> getCarsByManager(User manager) {
        return carRepository.findByManager(manager);
    }

    public List<Car> getAvailableCarsByManager(User manager) {
        return carRepository.findByManagerAndStatus(manager, Car.Status.AVAILABLE);
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