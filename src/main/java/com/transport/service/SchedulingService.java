package com.transport.service;

import com.transport.entity.Booking;
import com.transport.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SchedulingService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000)
    public void cancelUnpaidBookings() {
        logger.info("=== SCHEDULER STARTED ===");
        logger.info("Current time: {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        List<Booking> unpaidBookings = bookingRepository
                .findByStatusAndPaymentDeadlineBefore(Booking.Status.CONFIRMED, now);

        logger.info("Found {} unpaid bookings", unpaidBookings.size());

        for (Booking booking : unpaidBookings) {
            logger.info("Cancelling booking ID: {}, deadline was: {}",
                    booking.getId(), booking.getPaymentDeadline());
            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);
        }

        logger.info("=== SCHEDULER FINISHED ===");
    }
}