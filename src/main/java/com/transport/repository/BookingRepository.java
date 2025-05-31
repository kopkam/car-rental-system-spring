package com.transport.repository;

import com.transport.entity.Booking;
import com.transport.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomer(User customer);
    List<Booking> findByManager(User manager);
    List<Booking> findByStatus(Booking.Status status);
    List<Booking> findByStatusAndPaymentDeadlineBefore(Booking.Status status, LocalDateTime deadline);
}