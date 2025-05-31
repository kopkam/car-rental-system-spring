package com.transport.controller;

import com.transport.entity.Car;
import com.transport.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/cars")
public class CarController {

    @Autowired
    private CarService carService;

    @GetMapping
    public String listCars(Model model) {
        System.out.println("=== LOADING CARS PAGE ===");
        List<Car> cars = carService.getAllCars();
        model.addAttribute("cars", cars);
        System.out.println("Found " + cars.size() + " cars");
        return "cars/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String showAddForm(Model model) {
        model.addAttribute("car", new Car());
        return "cars/add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String addCar(@Valid @ModelAttribute Car car, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "cars/add";
        }

        carService.saveCar(car);
        return "redirect:/cars?success=true";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String showEditForm(@PathVariable Long id, Model model) {
        Car car = carService.getCarById(id);
        if (car == null) {
            return "redirect:/cars?error=notfound";
        }
        model.addAttribute("car", car);
        return "cars/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String updateCar(@PathVariable Long id, @Valid @ModelAttribute Car car, BindingResult result) {
        if (result.hasErrors()) {
            return "cars/edit";
        }

        car.setId(id);
        carService.saveCar(car);
        return "redirect:/cars?updated=true";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return "redirect:/cars?deleted=true";
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