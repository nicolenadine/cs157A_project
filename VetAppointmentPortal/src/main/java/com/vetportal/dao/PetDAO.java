package com.vetportal.dao;

import com.vetportal.mapper.PetMapper;
import com.vetportal.model.Pet;
import com.vetportal.exception.DataAccessException;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Extends the BaseDAO functionality to support Pet table specific implementation
 */
public class PetDAO extends BaseDAO<Pet> {

    private final CustomerDAO customerDAO;

    public PetDAO(Connection connection, CustomerDAO customerDAO) {
        super(connection, new PetMapper());
        this.customerDAO = customerDAO;
    }

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("pet_name", "species", "breed", "birth_date", "owner");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Pet pet) throws SQLException {
        setNonIdAttributes(statement, pet);
    }


    @Override
    protected void setUpdateStatement(PreparedStatement statement, Pet pet) throws SQLException {
        setNonIdAttributes(statement, pet);
        statement.setInt(6, pet.getID());
    }

    protected void setNonIdAttributes(PreparedStatement statement, Pet pet) throws SQLException {
        statement.setString(1, pet.getName());
        statement.setString(2, pet.getSpecies());
        statement.setString(3, pet.getBreed());
        statement.setString(4, pet.getBirthDate().toString());;
        statement.setInt(5, pet.getOwner().getID());
    }

    /**
     * Creates a pet and retrieves its autoincremented ID.
     * Uses the base class create method and does additional lookup if needed.
     *
     * @param pet the pet to create
     * @return Optional containing the created pet with ID
     * @throws DataAccessException if a database error occurs
     */
    public Optional<Pet> createPet(Pet pet) {
        try {
            boolean inserted = super.create(pet);
            if (!inserted) return Optional.empty();

            // IF DB says insertion was successfully but ID wasn't set for some reason then
            // attempt to set ID another way by looking up pet by other attributes that uniquely
            // identify pet (name, birthdate, owner)
            if (pet.getID() == 0) {

                //Sort ids so most recent (highest) is at top then return only the most recent id
                // which corresponds to the pet we just created.
                String sql = """
                   SELECT p.pet_id, p.pet_name, p.species, p.breed,\s
                   date(p.birth_date) as birth_date, p.owner, c.*
                   JOIN Customer c ON p.owner = c.customer_id
                   WHERE p.pet_name = ? AND p.birth_date = ? AND p.owner = ?
                   ORDER BY p.pet_id DESC LIMIT 1
                """;

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, pet.getName());
                    statement.setString(2, pet.getBirthDate().toString());
                    statement.setInt(3, pet.getOwner().getID());

                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        Pet newPet = mapper.mapResultSetToEntity(rs);
                        System.out.println("Found Pet with ID: " + newPet.getID());
                        pet.setID(newPet.getID()); // Set ID
                        return Optional.of(newPet);
                    }
                }
                return Optional.empty();
            }

            System.out.println("Created Pet with ID: " + pet.getID());
            return Optional.of(pet);
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DataAccessException("Error creating pet: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch a pet from the Pet table by the pet's ID
     *
     * @param id the integer corresponding to the pet's ID in the db
     * @return Optional containing the created pet with ID
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public Optional<Pet> findByID(Integer id) {
        String query = """
        SELECT p.pet_id, p.pet_name, p.species, p.breed,
        date(p.birth_date) as birth_date, p.owner, c.*
        FROM Pet p
        JOIN Customer c ON p.owner = c.customer_id
        WHERE p.pet_id = ?
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Pet pet = mapper.mapResultSetToEntity(rs);
                System.out.println("Found pet by ID " + id + ": Name=" + pet.getName() + ", Breed=" + pet.getBreed());
                return Optional.of(pet);
            } else {
                System.out.println("No pet found with ID: " + id);
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("Error fetching Pet by ID: " + e.getMessage());
            throw new DataAccessException("Error fetching Pet by ID", e);
        }
    }


    /**
     * Fetch a pet from the Pet table by the pet's ID
     *
     * @param attributes a Map of String pairs where the first String corresponds to
     *                   the Java object attribute and the second String corresponds
     *                   to the matching field in the db
     * @return Optional containing the created pet with ID
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public Optional<Pet> findByAttributes(Map<String, String> attributes) {
        Map<String, String> dbAttributes = translateAttributeNames(attributes);

        String conditions = dbAttributes.keySet().stream()
                .map(field -> "p." + field + " = ?")
                .collect(Collectors.joining(" AND "));

        String query = """
        SELECT p.pet_id, p.pet_name, p.species, p.breed,
        date(p.birth_date) as birth_date, p.owner,
        c.*
        FROM Pet p
        JOIN Customer c ON p.owner = c.customer_id
        WHERE
        """ + conditions;

        try {
            return executeQueryForList(query, dbAttributes.values().toArray()).stream().findFirst();
        } catch (SQLException e) {
            throw new DataAccessException("Error finding Pet by attributes", e);
        }
    }

    /**
     * Finds all pets owned by a customer based on customer ID.
     *
     * @param customerId the ID of the customer
     * @return a list of pets owned by the customer
     * @throws DataAccessException if a database error occurs
     */
    public List<Pet> findAllPetsByCustomerId(int customerId) {
        String query = """
        SELECT p.pet_id, p.pet_name, p.species, p.breed,
        date(p.birth_date) as birth_date, p.owner, c.*
        FROM Pet p
        JOIN Customer c ON p.owner = c.customer_id
        WHERE p.owner = ?
    """;

        List<Pet> pets = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, customerId);
            System.out.println("Executing query for pets with customer ID: " + customerId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Pet pet = mapper.mapResultSetToEntity(rs);
                System.out.println("Found pet for customer " + customerId + ": ID=" + pet.getID() + ", Name=" + pet.getName());
                pets.add(pet);
            }

            System.out.println("Total pets found for customer " + customerId + ": " + pets.size());

        } catch (SQLException e) {
            System.err.println("Error in findAllPetsByCustomerId: " + e.getMessage());
            throw new DataAccessException("Error finding pets by customer ID", e);
        }

        return pets;
    }

    /**
     * Helper method to facilitate filtering based on multiple WHERE conditions
     *
     *
     * @param dbFields a Map of String pairs where the first string corresponds to the
     *                 Java object attribute and the second String corresponds to the
     *                 matching field in the db
     * @return String containing the full SQL query with all conditions appended
     * @throws DataAccessException if a database error occurs
     */
    @Override
    protected String buildSelectQuery(Map<String, String> dbFields) {
        String[] conditions = dbFields.keySet().stream()
                .map(field -> "p." + field + " = ?")
                .toArray(String[]::new);

        return """
        SELECT p.pet_id, p.pet_name, p.species, p.breed,
        date(p.birth_date) as birth_date, p.owner, c.*
        JOIN Customer c ON p.owner = c.customer_id
        WHERE %s
    """.formatted(String.join(" AND ", conditions));
    }
}