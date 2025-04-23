package com.vetportal.model;

public class Pet {
    private int id;
    private String name;
    private String species;
    private String breed;
    private String birthDate;
    private Customer owner;

    public Pet(int id, String name, String species, String breed, String birthDate, Customer owner) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public Customer getOwner() {
        return owner;
    }

    public void setOwner(Customer owner) {
        this.owner = owner;
    }
}