package com.transport.controller;

import com.transport.entity.Booking;
import com.transport.entity.Car;
import com.transport.entity.User;
import com.transport.service.BookingService;
import com.transport.service.CarService;
import com.transport.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CarService carService;

    @Autowired
    private PdfService pdfService;

    @GetMapping
    public String listBookings(Authentication auth, Model model) {
        User user = (User) auth.getPrincipal();
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
    public String showBookingForm(Model model) {
        List<Car> availableCars = carService.findByStatus(Car.Status.AVAILABLE);
        model.addAttribute("cars", availableCars);
        model.addAttribute("booking", new Booking());
        return "booking-form";
    }

    @PostMapping
    public String createBooking(@ModelAttribute Booking booking, Authentication auth) {
        User customer = (User) auth.getPrincipal();
        booking.setCustomer(customer);
        bookingService.save(booking);
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id) {
        Booking booking = bookingService.findById(id);
        booking.setStatus(Booking.Status.CONFIRMED);
        bookingService.save(booking);
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id) {
        Booking booking = bookingService.findById(id);
        booking.setStatus(Booking.Status.CANCELLED);
        bookingService.save(booking);
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/pay")
    public String payBooking(@PathVariable Long id) {
        Booking booking = bookingService.findById(id);
        booking.setStatus(Booking.Status.PAID);
        bookingService.save(booking);
        bookingService.generateInvoice(booking);
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        Booking booking = bookingService.findById(id);
        byte[] pdfContent = pdfService.generateInvoicePdf(booking);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + booking.getId() + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }
}