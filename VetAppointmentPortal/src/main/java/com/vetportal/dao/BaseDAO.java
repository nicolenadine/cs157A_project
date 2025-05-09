package com.vetportal.dao;

import com.vetportal.mapper.EntityMapper;
import com.vetportal.exception.DataAccessException;
import com.vetportal.util.DbManager;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base abstract DAO implementation providing common CRUD operations.
 * Child classes should extend this to use for core functionality
 * only implementing entity-specific methods.
 *
 * @param <T> the entity type this DAO manages
 */
public abstract class BaseDAO<T> implements GenericDAO<T> {

    protected Connection connection;
    protected EntityMapper<T> mapper;


    /**
     * Constructs a new BaseDAO with the given database connection and entity mapper.
     * Ensures that foreign key constraints are enabled.
     *
     * @param connection the shared active database connection
     * @param mapper the entity mapper for converting between database records and Java class attributes
     */
    public BaseDAO(Connection connection, EntityMapper<T> mapper) {
        this.connection = connection;
        this.mapper = mapper;

        try {
            DbManager.ensureForeignKeysEnabled();
        } catch (SQLException e) {
            System.err.println("Warning: Failed to ensure foreign keys are enabled: " + e.getMessage());
        }
    }


    // --------------------  ABSTRACT METHODS -------------------


    /**
     * Returns the ordered list of database column names for this entity type.
     * Used to construct SQL statements because column order matters
     *
     * @return a list of database column names in the order they must appear in SQL statements
     *      for the table corresponding to this entity
     */
    protected abstract List<String> getOrderedAttributes();

    /**
     * Sets parameters for a SQL insert statement using values from the Java entity.
     *
     * @param statement the prepared statement to set parameters for
     * @param entity the entity containing values to insert in the db
     * @throws SQLException if a database access error occurs
     */
    protected abstract void setCreateStatement(PreparedStatement statement, T entity) throws SQLException;


    /**
     * Sets parameters for a SQL update statement using values from the entity.
     *
     * @param statement the prepared statement to set parameters for
     * @param entity the entity containing values to update in the corresponding db entry
     * @throws SQLException if a database access error occurs
     */
    protected abstract void setUpdateStatement(PreparedStatement statement, T entity) throws SQLException;


    // ----------------------  CONCRETE METHODS --------------------


    /**
     * Helper method that builds an SQL INSERT query for this entity type.
     * Uses the ordered attribute list to ensure consistent column order.
     *
     * @return a SQL INSERT statement with placeholders for values
     */
    protected String getCreateQuery() {
        String tableName = mapper.getTableName();
        List<String> orderedAttributes = getOrderedAttributes();

        // A string containing the column fields in order separated by a comma
        String columns = String.join(", ", orderedAttributes);

        // A string containing a number of '?' placeholder symbols equal to the number of attributes, separated by comma
        String placeholders = String.join(", ", Collections.nCopies(orderedAttributes.size(), "?"));

        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }


    /**
     * Helper method that builds an SQL UPDATE query for this entity type.
     * Uses the ordered attribute list to ensure consistent column order.
     *
     * @return a SQL UPDATE statement with placeholders for values
     *          and the ID for the WHERE clause
     */
    protected String getUpdateQuery() {
        String tableName = mapper.getTableName();
        Map<String, String> attributeMap = mapper.getJavaToDbAttributeMap();

        // Get the database field name corresponding to 'id'  (e.g, customer_id, pet_id, ...)
        String idColumn = attributeMap.get("id");

        List<String> columns = getOrderedAttributes();

        // Update statements must include all fields. For each column in the ordered attributes
        // append a '= ?' after the column (e.g, 'first_name' becomes 'first_name = ?')
        // then combines these column strings separated by a comma.
        String setClause = columns.stream()
                .map(col -> col + " = ?")
                .collect(Collectors.joining(", "));

        return "UPDATE " + tableName + " SET " + setClause + " WHERE " + idColumn + " = ?";
    }


    /**
     * Helper method that builds an SQL DELETE query for this entity type.
     * Uses the ID column from the entity mapper for the WHERE clause.
     *
     * @return a SQL DELETE statement with a placeholder for the ID to go in WHERE clause
     */
    protected String getDeleteQuery() {
        String tableName = mapper.getTableName();
        Map<String, String> attributeMap = mapper.getJavaToDbAttributeMap();

        // Need the database id field name for the class to use in the WHERE statement
        String idColumn = attributeMap.get("id");

        return "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";
    }


    /**
     * Helper method that builds an SQL SELECT query to find all entities of this type.
     *
     * @return a SQL SELECT statement to retrieve all records
     */
    protected String getFindAllQuery() {
        return "SELECT * FROM " + mapper.getTableName();
    }


    /**
     * Helper method builds an SQL SELECT query with conditions based on the provided attributes.
     *
     * Takes a map of database fields and their values and turns them into 'column = ?' conditions
     *      joined by AND
     *
     * @param dbAttributes a Map of String pairs where the first String corresponds to the
     *                     database field name and the second String corresponds to the value to filter by
     * @return a SQL SELECT statement with placeholders for the attribute values
     */
    protected String buildSelectQuery(Map<String, String> dbAttributes) {

        // Creates a stream of attribute values and for each one appends the placeholder " = ?" after
        // These conditions are inserted into a String[] sized to the number of attributes in the stream
        String[] conditions = dbAttributes.keySet().stream()
                .map(field -> field + " = ?")
                .toArray(String[]::new);

        // after the WHERE keyword add each condition in the array joined with "AND" to complete the query
        return "SELECT * FROM " + mapper.getTableName() + " WHERE " + String.join(" AND ", conditions);
    }


    /**
     * Executes a SQL query and maps the results to entities.
     *
     * Takes a complete SQL query string and parameter values , sets the values in the prepared
     *      statement and executes it. Maps the results to their corresponding Java entities
     *
     * @param query the SQL query to execute
     * @param params the parameter values to use in the query
     * @return a list of entities matching the query
     * @throws SQLException if a database error occurs
     */
    protected List<T> executeQueryForList(String query, Object[] params) throws SQLException {
        // This method is designed to be as generic as possible in order
        // to work with any class, entity type, or number of parameters

        List<T> results = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {

            // Loop through the array of parameters (values to be used in the query)
            for (int i = 0; i < params.length; i++) {
                // set query values based on params.
                // indexing starts at 1 not 0 in JDBC
                statement.setObject(i + 1, params[i]);
            }

            ResultSet rs = statement.executeQuery();

            // Retrieve and all results while rs has next
            while (rs.next()) {
                try {
                    // Uses mapper class of subclass T (all subclasses have defined mappers)
                    T entity = mapper.mapResultSetToEntity(rs);
                    results.add(entity);
                } catch (SQLException e) {
                    System.err.println("Error mapping entity from result set: " + e.getMessage());
                    throw e;
                }
            }
        }

        return results;
    }


    /**
     * Translates Java attribute names to database column names using the entity's mapper.
     *
     * @param attributes a Map of Strings where the first String corresponds to the Java entity attributes
     *                   and the second String corresponds to the attribute's value
     * @return a Map of Strings where the first String corresponds to the  database column name  and the second
     *          String corresponds to the field's value
     * @throws IllegalArgumentException if an invalid attribute name is provided
     */
    protected Map<String, String> translateAttributeNames(Map<String, String> attributes) {

        // dbAttributes Will store the database field names and corresponding values passed by caller
        Map<String, String> dbAttributes = new HashMap<>();
        Map<String, String> attributeMap = mapper.getJavaToDbAttributeMap();

        // For each attribute:value pair get the Java attribute name (key)
        // and use that key to get corresponding db field name from the mapper.
        // Add the retrieved db field and attribute value to the dbAttributes Map
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String javaAttr = entry.getKey();
            String dbAttr = attributeMap.get(javaAttr);

            if (dbAttr == null) {
                throw new IllegalArgumentException("Invalid attribute: " + javaAttr);
            }

            // Add db field and assigned value to the HashMap
            dbAttributes.put(dbAttr, entry.getValue());
        }

        return dbAttributes;
    }


    /**
     * Creates a new entity in the database.
     * Attempts to set the assigned autoincremented ID in the entity object.
     *
     * @param entity the entity to create
     * @return true if the entity was created successfully, false otherwise
     * @throws DataAccessException if insert is a duplicate or if another database error occurs
     */
    @Override
    public boolean create(T entity) {
        String sql = getCreateQuery();
        System.out.println("Executing SQL: " + sql);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // use subclass's setCreateStatement method to assign entity's values to
            // statement placeholders
            setCreateStatement(statement, entity);

            int rowsAffected = statement.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

            if (rowsAffected > 0) {
                try (Statement idStatement = connection.createStatement();

                     // Since id is autoincremented by db we need to retrieve the assigned id
                     ResultSet rs = idStatement.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int newEntryID = rs.getInt(1); // first column of current row of rs = the id field
                        System.out.println("Last inserted ID: " + newEntryID);

                        try {
                            // We don't know what specific type it will be until runtime. So this uses Java's reflection methods
                            // to get the setID method for the entity object and calls it, passing the newly assigned ID value
                            // It is important for every entity class to have a setID method.
                            java.lang.reflect.Method setIdMethod = entity.getClass().getMethod("setID", Integer.class);
                            setIdMethod.invoke(entity, newEntryID);
                        } catch (Exception e) {
                            System.out.println("Could not set ID on entity: " + e.getMessage());
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("SQL Error in create(): " + e.getMessage());
            // Checks for UNIQUE constraint violations
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new DataAccessException("Duplicate entry detected: " + e.getMessage(), e);
            }
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }


    /**
     * Updates an existing entity in the database.
     *
     * @param entity the entity to update
     * @return true if the entity was updated successfully, false otherwise
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public boolean update(T entity) {
        // Each subclass is responsible for setting the update and set  query parameters since
        // Each table has a different number of attributes of different types
        String sql = getUpdateQuery();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setUpdateStatement(statement, entity);

            int rowsAffected = statement.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("SQL Error in update(): " + e.getMessage());
            e.printStackTrace();
            throw new DataAccessException("Error updating record: " + e.getMessage(), e);
        }
    }


    /**
     * Deletes an entity from the database by ID.
     * Ensures that foreign key constraints are enabled to properly cascade deletions.
     *
     * @param id the ID of the entity to delete
     * @return true if the entity was deleted successfully, false otherwise
     * @throws DataAccessException if foreign keys are not enabled or a database error occurs
     */
    @Override
    public boolean delete(Integer id) {

        // Important that foreign keys are enabled because many tables have ON DELETE CASCADE
        try {
            DbManager.ensureForeignKeysEnabled();
        } catch (SQLException e) {
            throw new DataAccessException("Foreign keys not enabled: " + e.getMessage());
        }

        String sql = getDeleteQuery();
        System.out.println("Executing SQL: " + sql + " with ID: " + id);


        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id); // only need id (primary key) to execute delete
            int rowsAffected = statement.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("SQL Error in delete(): " + e.getMessage());
            e.printStackTrace();
            throw new DataAccessException("Error deleting record: " + e.getMessage(), e);
        }
    }


    /**
     * Finds an entity by its ID.
     * Uses the findByAttributes method with a map containing just the ID.
     *
     * @param id the ID of the entity to find
     * @return an Optional containing the found entity, or empty if not found
     * @throws DataAccessException if a database error occurs
     * @throws IllegalStateException if it can't properly map id attribute to db field
     */
    @Override
    public Optional<T> findByID(Integer id) {
        Map<String, String> attributeMap = mapper.getJavaToDbAttributeMap();
        String dbIdField = attributeMap.get("id");

        // This should never happen but just in case
        if (dbIdField == null) {
            throw new IllegalStateException("Missing 'id' field mapping");
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put("id", String.valueOf(id));

        // Calls findByAttributes just with only id attribute supplied
        return findByAttributes(attributes);
    }


    /**
     * Retrieves all entities of this type from the database.
     *
     * @return a list of all entities
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public List<T> findAll() {
        List<T> entities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(getFindAllQuery());
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                entities.add(mapper.mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching all records from " + mapper.getTableName(), e);
        }
        return entities;
    }


    /**
     * Finds an entity by specific attribute values.
     * Translates Java attribute names to database column names and builds a query.
     *
     * @param attributes a Map of Strings where the first String corresponds to entity attribute names
     *                   and the second String corresponds to values to search for in db
     * @return an Optional containing the found entity, or empty if not found
     * @throws DataAccessException if a database error occurs
     */
    public Optional<T> findByAttributes(Map<String, String> attributes) {
        // Convert Java attribute names to database column names
        Map<String, String> dbAttributes = translateAttributeNames(attributes);

        // Create the SELECT query with WHERE conditions for each attribute
        String query = buildSelectQuery(dbAttributes);

        try {
            // Execute the query with attribute values as parameters
            // and convert the result set into a list of entity objects
            // attribute values are converted to array because executeQueryForList takes
            // an Object[] of parameters
            List<T> results = executeQueryForList(query, dbAttributes.values().toArray());

            // If no results found, return an empty Optional
            if (results.isEmpty()) {
                return Optional.empty();
            }

            // Return the first result wrapped in an Optional
            return Optional.of(results.get(0));
        } catch (SQLException e) {
            throw new DataAccessException("Error finding by attributes: " + e.getMessage(), e);
        }
    }


    /**
     * Finds all entities matching specific attribute values.
     * Translates Java attribute names to database column names and builds a query.
     *
     * @param attributes  a Map of Strings where the first String corresponds to entity attribute names
     *                    and the second String corresponds to values to search for in db
     * @return a list of matching entities
     * @throws DataAccessException if a database error occurs
     */
    public List<T> findAllByAttributes(Map<String, String> attributes) {
        // Convert Java attribute/field names to database column names
        Map<String, String> dbAttributes = translateAttributeNames(attributes);

        // Create a SELECT query with WHERE conditions for each attribute
        String query = buildSelectQuery(dbAttributes);

        try {
            // Execute the query with the attribute values as parameters. Attribute values are
            // converted to array because executeQueryForList takes an Object[] of parameters.
            // convert the result set into a list of entity objects
            return executeQueryForList(query, dbAttributes.values().toArray());
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching entities by attributes", e);
        }
    }
}
