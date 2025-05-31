package com.transport.service;

import com.transport.entity.Car;
import com.transport.entity.User;
import com.transport.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CarService {

    @Autowired
    private CarRepository carRepository;

    public List<Car> findAll() {
        return carRepository.findAll();
    }

    public List<Car> findByManager(User manager) {
        return carRepository.findByManager(manager);
    }

    public List<Car> findByStatus(Car.Status status) {
        return carRepository.findByStatus(status);
    }

    public Car findById(Long id) {
        return carRepository.findById(id).orElse(null);
    }

    public Car save(Car car) {
        return carRepository.save(car);
    }

    public void deleteById(Long id) {
        carRepository.deleteById(id);
    }
}