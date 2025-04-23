package com.vetportal.dao.impl;

import com.vetportal.model.Customer;
import com.vetportal.dao.impl.CustomerDAO;
import com.vetportal.model.Pet;
import com.vetportal.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PetDAO extends BaseDAO<Pet> {

    private final CustomerDAO customerDAO;

    public PetDAO(Connection connection, CustomerDAO customerDAO) {
        super(connection);
        this.customerDAO = customerDAO;
    }

    @Override
    protected String getTableName() {
        return "pet";
    }

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("pet_id", "name", "species", "breed", "birth_date", "customer_id"); // Exclude pet_id
    }

    @Override
    protected Set<String> getAllowedAttributes() {
        return Set.of("pet_id", "name", "species", "breed", "birth_date", "customer_id");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Pet pet) throws SQLException {
        statement.setInt(5, pet.getOwner().getId());
        statement.setString(1, pet.getName());
        statement.setString(2, pet.getSpecies());
        statement.setString(3, pet.getBreed());
        statement.setDate(4, java.sql.Date.valueOf(pet.getBirthDate())); // assuming LocalDate
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

    @Override
    protected Pet extractEntityFromResultSet(ResultSet rs) throws SQLException {

        int customerId = rs.getInt("customer_id");

        Customer owner = customerDAO.findByID(customerId); // assumes this returns a full Customer

        return new Pet(
                rs.getInt("pet_id"),
                rs.getString("name"),
                rs.getString("species"),
                rs.getString("breed"),
                rs.getDate("birth_date").toString(),
                owner
        );
    }

    public Optional<Pet> findPetByIdAndCustomerId(int petId, int customerId) {
        String sql = """
        SELECT p.*, c.*
        FROM pet p
        JOIN customer c ON p.customer_id = c.id
        WHERE p.pet_id = ? AND p.customer_id = ?
    """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, petId);
            statement.setInt(2, customerId);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Pet pet = extractEntityFromResultSet(rs);
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
        FROM pet p
        JOIN customer c ON p.customer_id = c.id
        WHERE p.customer_id = ?
    """;

        List<Pet> pets = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Customer owner = new Customer(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address")
                );

                Pet pet = new Pet(
                        rs.getInt("pet_id"),
                        rs.getString("name"),
                        rs.getString("species"),
                        rs.getString("breed"),
                        rs.getDate("birth_date").toString(),
                        owner
                );

                pets.add(pet);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error finding pets by customer ID", e);
        }

        return pets;
    }


}
