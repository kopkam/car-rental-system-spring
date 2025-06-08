package com.transport.controller;

import com.transport.entity.Car;
import com.transport.entity.User;
import com.transport.service.CarService;
import com.transport.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/cars")
public class CarController {

    @Autowired
    private CarService carService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listCars(Authentication auth, Model model) {
        System.out.println("=== LOADING CARS PAGE ===");

        String username = auth.getName();
        User currentUser = userService.findByUsername(username);

        List<Car> cars;

        // ADMIN widzi wszystkie auta
        if (currentUser.hasRole("ROLE_ADMIN")) {
            cars = carService.getAllCars();
        }
        // MANAGER widzi tylko swoje auta
        else if (currentUser.hasRole("ROLE_MANAGER")) {
            cars = carService.getCarsByManager(currentUser);
        }
        // CUSTOMER widzi wszystkie auta (tylko do przeglądania)
        else {
            cars = carService.getAllCars();
        }

        model.addAttribute("cars", cars);
        model.addAttribute("currentUser", currentUser);
        System.out.println("Found " + cars.size() + " cars for user: " + username);
        return "cars/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String showAddForm(Authentication auth, Model model) {
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);

        model.addAttribute("car", new Car());
        model.addAttribute("currentUser", currentUser);

        // Jeśli admin, dodaj listę managerów do wyboru
        if (currentUser.hasRole("ROLE_ADMIN")) {
            List<User> managers = userService.findByRole("ROLE_MANAGER");
            model.addAttribute("managers", managers);
        }

        return "cars/add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String addCar(@Valid @ModelAttribute Car car,
                         BindingResult result,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            model.addAttribute("currentUser", currentUser);

            if (currentUser.hasRole("ROLE_ADMIN")) {
                List<User> managers = userService.findByRole("ROLE_MANAGER");
                model.addAttribute("managers", managers);
            }
            return "cars/add";
        }

        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            carService.saveCar(car, currentUser, imageFile);

            redirectAttributes.addFlashAttribute("success", "Car added successfully!");
            return "redirect:/cars";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding car: " + e.getMessage());
            return "redirect:/cars/add";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String showEditForm(@PathVariable Long id, Authentication auth, Model model) {
        Car car = carService.getCarById(id);
        if (car == null) {
            return "redirect:/cars?error=notfound";
        }

        String username = auth.getName();
        User currentUser = userService.findByUsername(username);

        // Manager może edytować tylko swoje auta
        if (currentUser.hasRole("ROLE_MANAGER")) {
            if (car.getManager() == null || !car.getManager().getId().equals(currentUser.getId())) {
                return "redirect:/cars?error=unauthorized";
            }
        }

        model.addAttribute("car", car);
        model.addAttribute("currentUser", currentUser);

        // Jeśli admin, dodaj listę managerów do wyboru
        if (currentUser.hasRole("ROLE_ADMIN")) {
            List<User> managers = userService.findByRole("ROLE_MANAGER");
            model.addAttribute("managers", managers);
        }

        return "cars/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String updateCar(@PathVariable Long id,
                            @Valid @ModelAttribute Car car,
                            BindingResult result,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            model.addAttribute("currentUser", currentUser);

            if (currentUser.hasRole("ROLE_ADMIN")) {
                List<User> managers = userService.findByRole("ROLE_MANAGER");
                model.addAttribute("managers", managers);
            }
            return "cars/edit";
        }

        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            // Sprawdź uprawnienia managera
            Car existingCar = carService.getCarById(id);
            if (currentUser.hasRole("ROLE_MANAGER")) {
                if (existingCar.getManager() == null || !existingCar.getManager().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You can only edit your own cars!");
                    return "redirect:/cars";
                }
            }

            car.setId(id);

            // Logika zarządzania managerem
            if (currentUser.hasRole("ROLE_MANAGER")) {
                // Manager może edytować tylko swoje auta - przypisz siebie
                car.setManager(currentUser);
            } else if (currentUser.hasRole("ROLE_ADMIN")) {

                if (car.getManager() == null || car.getManager().getId() == null) {
                    car.setManager(null); // Usuń managera - auto będzie zarządzane przez admina
                } else {

                    User selectedManager = userService.findById(car.getManager().getId());
                    car.setManager(selectedManager);
                }
            }

            if (imageFile == null || imageFile.isEmpty()) {
                car.setImageUrl(existingCar.getImageUrl());
            }

            carService.saveCar(car, currentUser, imageFile);
            redirectAttributes.addFlashAttribute("success", "Car updated successfully!");
            return "redirect:/cars";
        } catch (Exception e) {
            System.out.println("❌ ERROR updating car: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating car: " + e.getMessage());
            return "redirect:/cars/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String deleteCar(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            // Sprawdź uprawnienia managera
            Car car = carService.getCarById(id);
            if (currentUser.hasRole("ROLE_MANAGER")) {
                if (car.getManager() == null || !car.getManager().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You can only delete your own cars!");
                    return "redirect:/cars";
                }
            }

            carService.deleteCar(id);
            redirectAttributes.addFlashAttribute("success", "Car deleted successfully!");
            return "redirect:/cars";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting car: " + e.getMessage());
            return "redirect:/cars";
        }
    }

    @GetMapping("/view/{id}")
    public String viewCar(@PathVariable Long id, Model model) {
        Car car = carService.getCarById(id);
        if (car == null) {
            return "redirect:/cars?error=notfound";
        }
        model.addAttribute("car", car);
        return "cars/view";
    }
}