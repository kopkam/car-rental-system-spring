package com.transport.service;

import com.transport.entity.Booking;
import com.transport.entity.Invoice;
import com.transport.entity.User;
import com.transport.repository.BookingRepository;
import com.transport.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private EmailService emailService;

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public List<Booking> findByCustomer(User customer) {
        return bookingRepository.findByCustomer(customer);
    }

    public List<Booking> findByManager(User manager) {
        return bookingRepository.findByManager(manager);
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public Booking save(Booking booking) {
        // Calculate total amount
        if (booking.getTotalAmount() == null) {
            long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
            BigDecimal totalAmount = booking.getCar().getDailyRate().multiply(BigDecimal.valueOf(days));
            booking.setTotalAmount(totalAmount);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Send confirmation email
        if (booking.getStatus() == Booking.Status.CONFIRMED) {
            emailService.sendBookingConfirmation(booking.getCustomer(), booking.getId());
        }

        return savedBooking;
    }

    public void deleteById(Long id) {
        bookingRepository.deleteById(id);
    }

    public void generateInvoice(Booking booking) {
        if (booking.getInvoice() == null) {
            String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            BigDecimal taxAmount = booking.getTotalAmount().multiply(BigDecimal.valueOf(0.21)); // 21% VAT

            Invoice invoice = new Invoice(invoiceNumber, booking, booking.getTotalAmount(), taxAmount);
            invoice = invoiceRepository.save(invoice);

            booking.setInvoice(invoice);
            bookingRepository.save(booking);
        }
    }
}