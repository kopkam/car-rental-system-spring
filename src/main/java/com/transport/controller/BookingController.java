package com.transport.controller;

import com.transport.entity.Booking;
import com.transport.entity.Car;
import com.transport.entity.User;
import com.transport.service.BookingService;
import com.transport.service.CarService;
import com.transport.service.PdfService;
import com.transport.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping({"/bookings", "/reservations"}) // obsługuje oba URL-e
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CarService carService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listBookings(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username);

        List<Booking> bookings;

        if (user.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"))) {
            bookings = bookingService.findAll();
        } else if (user.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_MANAGER"))) {
            bookings = bookingService.findByManager(user);
        } else {
            bookings = bookingService.findByCustomer(user);
        }

        model.addAttribute("bookings", bookings);
        return "bookings";
    }

    @GetMapping("/new")
    public String showBookingForm(Authentication auth, Model model) {
        // Pobierz dostępne samochody
        List<Car> availableCars = carService.getCarsByStatus(Car.Status.AVAILABLE);

        // Pobierz aktualnie zalogowanego użytkownika
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);

        // Przekaż dane do formularza
        model.addAttribute("availableCars", availableCars);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("booking", new Booking());

        return "booking-form";
    }

    @PostMapping
    public String createBooking(@ModelAttribute Booking booking, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            // Pobierz użytkownika z bazy danych
            String username = auth.getName();
            User customer = userService.findByUsername(username);

            booking.setCustomer(customer);
            booking.setStatus(Booking.Status.PENDING); // Ustaw status na PENDING

            // 🚗 ZMIEŃ STATUS AUTA NA RENTED
            Car car = booking.getCar();
            if (car != null) {
                car.setStatus(Car.Status.RENTED);
                carService.save(car); // Zapisz auto z nowym statusem
            }

            bookingService.save(booking);
            redirectAttributes.addFlashAttribute("success", "Booking created successfully! Car is now rented.");
            return "redirect:/bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/bookings/new";
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id);

            if (booking.getStatus() == Booking.Status.PENDING) {
                booking.setStatus(Booking.Status.CONFIRMED);

                // Upewnij się że auto jest RENTED
                Car car = booking.getCar();
                if (car != null && car.getStatus() != Car.Status.RENTED) {
                    car.setStatus(Car.Status.RENTED);
                    carService.save(car);
                }

                bookingService.save(booking);
                redirectAttributes.addFlashAttribute("success", "Booking confirmed successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Only pending bookings can be confirmed.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error confirming booking: " + e.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id);
            Booking.Status oldStatus = booking.getStatus();

            if (oldStatus == Booking.Status.PENDING ||
                    oldStatus == Booking.Status.CONFIRMED ||
                    oldStatus == Booking.Status.PAID) {

                booking.setStatus(Booking.Status.CANCELLED);

                // 🚗 ZWOLNIJ SAMOCHÓD - ustaw na AVAILABLE
                Car car = booking.getCar();
                if (car != null) {
                    car.setStatus(Car.Status.AVAILABLE);
                    carService.save(car);
                }

                bookingService.save(booking);
                redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully! Car is now available.");
            } else {
                redirectAttributes.addFlashAttribute("error", "This booking cannot be cancelled.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/pay")
    public String payBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id);

            if (booking.getStatus() == Booking.Status.CONFIRMED) {
                booking.setStatus(Booking.Status.PAID);

                // Auto już powinno być RENTED, ale sprawdźmy
                Car car = booking.getCar();
                if (car != null && car.getStatus() != Car.Status.RENTED) {
                    car.setStatus(Car.Status.RENTED);
                    carService.save(car);
                }

                bookingService.save(booking);
                bookingService.generateInvoice(booking);
                redirectAttributes.addFlashAttribute("success", "Booking paid successfully! Invoice generated.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Only confirmed bookings can be paid.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing payment: " + e.getMessage());
        }
        return "redirect:/bookings";
    }

    // 🆕 NOWA METODA - COMPLETE BOOKING
    @PostMapping("/{id}/complete")
    public String completeBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id);

            if (booking.getStatus() == Booking.Status.PAID ||
                    booking.getStatus() == Booking.Status.CONFIRMED) {

                booking.setStatus(Booking.Status.COMPLETED);

                // 🚗 ZWOLNIJ SAMOCHÓD - ustaw na AVAILABLE
                Car car = booking.getCar();
                if (car != null) {
                    car.setStatus(Car.Status.AVAILABLE);
                    carService.save(car);
                }

                bookingService.save(booking);
                redirectAttributes.addFlashAttribute("success", "Booking completed successfully! Car is now available.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Only paid/confirmed bookings can be completed.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error completing booking: " + e.getMessage());
        }
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        Booking booking = bookingService.findById(id);
        byte[] pdfContent = pdfService.generateInvoicePdf(booking);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "invoice-" + booking.getId() + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }
}