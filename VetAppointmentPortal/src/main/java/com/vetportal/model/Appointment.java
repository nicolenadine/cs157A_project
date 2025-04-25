package com.vetportal.model;

public class Appointment {
    private Integer id;
    private String date;
    private String time;
    private Employee provider;
    private AppointmentType appointmentType;
    private Pet pet;
    private Customer customer;

    public Appointment(Integer id, String date, String time, Employee provider, AppointmentType appointmentType, Pet pet, Customer customer) {
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
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
