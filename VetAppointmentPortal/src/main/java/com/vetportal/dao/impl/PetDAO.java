package com.vetportal.dao.impl;

import com.vetportal.mapper.PetMapper;
import com.vetportal.model.Customer;
import com.vetportal.dao.impl.CustomerDAO;
import com.vetportal.model.Pet;
import com.vetportal.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PetDAO extends BaseDAO<Pet> {

    private final CustomerDAO customerDAO;

    public PetDAO(Connection connection, CustomerDAO customerDAO) {
        super(connection, new PetMapper());
        this.customerDAO = customerDAO;
    }

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("pet_id", "name", "species", "breed", "birth_date", "owner");
    }

    @Override
    protected Set<String> getAllowedAttributes() {
        return Set.of("pet_id", "name", "species", "breed", "birth_date", "owner");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Pet pet) throws SQLException {
        statement.setInt(1, pet.getId());
        statement.setString(2, pet.getName());
        statement.setString(3, pet.getSpecies());
        statement.setString(4, pet.getBreed());
        statement.setDate(5, java.sql.Date.valueOf(pet.getBirthDate()));
        statement.setInt(6, pet.getOwner().getId());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Pet pet) throws SQLException {
        statement.setString(1, pet.getName());
        statement.setString(2, pet.getSpecies());
        statement.setString(3, pet.getBreed());
        statement.setDate(4, java.sql.Date.valueOf(pet.getBirthDate()));
        statement.setInt(5, pet.getOwner().getId());
        statement.setInt(6, pet.getId());
    }


    public Optional<Pet> findPetByIdAndCustomerId(int petId, int customerId) {
        String sql = """
        SELECT p.*, c.*
        FROM Pet p
        JOIN Customer c ON p.owner = c.id
        WHERE p.pet_id = ? AND p.owner= ?
    """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, petId);
            statement.setInt(2, customerId);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {

                Pet pet = mapper.mapResultSetToEntity(rs);

                return Optional.of(pet);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error finding pet by ID and customer ID", e);
        }

        return Optional.empty();
    }

    public List<Pet> findAllPetsByCustomerId(int customerId) {
        String sql = """
        SELECT p.*, c.*
        FROM Pet p
        JOIN Customer c ON p.owner = c.customer_id
        WHERE p.owner = ?
    """;

        List<Pet> pets = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Pet pet = mapper.mapResultSetToEntity(rs);
                pets.add(pet);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error finding pets by customer ID", e);
        }

        return pets;
    }

    @Override
    protected String buildSelectQuery(Map<String, String> dbFields) {
        String[] conditions = dbFields.keySet().stream()
                .map(field -> "p." + field + " = ?")
                .toArray(String[]::new);

        return """
        SELECT p.*, c.*
        FROM Pet p
        JOIN Customer c ON p.owner = c.customer_id
        WHERE %s
    """.formatted(String.join(" AND ", conditions));
    }

}
