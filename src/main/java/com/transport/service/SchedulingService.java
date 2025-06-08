package com.transport.service;

import com.transport.entity.Booking;
import com.transport.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SchedulingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cancelUnpaidBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> unpaidBookings = bookingRepository
                .findByStatusAndPaymentDeadlineBefore(Booking.Status.CONFIRMED, now);

        for (Booking booking : unpaidBookings) {
            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);
        }
    }
}