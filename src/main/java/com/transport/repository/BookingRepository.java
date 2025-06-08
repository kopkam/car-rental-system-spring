package com.transport.repository;

import com.transport.entity.Booking;
import com.transport.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomer(User customer);
    List<Booking> findByManager(User manager);

    // Rezerwacje aut danego managera
    @Query("SELECT b FROM Booking b WHERE b.car.manager = :manager")
    List<Booking> findByCarManager(@Param("manager") User manager);

    List<Booking> findByStatus(Booking.Status status);
    List<Booking> findByStatusAndPaymentDeadlineBefore(Booking.Status status, LocalDateTime deadline);

    @Query("SELECT b FROM Booking b WHERE b.car.manager = :manager AND b.status = :status")
    List<Booking> findByCarManagerAndStatus(@Param("manager") User manager, @Param("status") Booking.Status status);

    List<Booking> findByManagerAndStatus(User manager, Booking.Status status);
}