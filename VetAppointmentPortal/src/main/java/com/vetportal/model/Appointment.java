package com.vetportal.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private Integer id;
    private LocalDate date;
    private LocalTime time;
    private Employee provider;
    private AppointmentType appointmentType;
    private Pet pet;
    private Customer customer;

    public Appointment(Integer id, LocalDate date, LocalTime time, Employee provider, AppointmentType appointmentType, Pet pet, Customer customer) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.provider = provider;
        this.appointmentType = appointmentType;
        this.pet = pet;
        this.customer = customer;
    }

    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Employee getProvider() {
        return provider;
    }

    public void setProvider(Employee provider) {
        this.provider = provider;
    }

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(AppointmentType appointmentType) {
        this.appointmentType = appointmentType;
    }

    public Pet getPet() {
        return pet;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
