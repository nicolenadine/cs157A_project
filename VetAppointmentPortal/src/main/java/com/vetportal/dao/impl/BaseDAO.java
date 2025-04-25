package com.vetportal.dao.impl;

import com.vetportal.dao.interfaces.GenericDAO;
import com.vetportal.mapper.EntityMapper;
import com.vetportal.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseDAO<T> implements GenericDAO<T> {
    protected Connection connection;
    protected EntityMapper<T> mapper;

    // Constructor
    public BaseDAO(Connection connection, EntityMapper<T> mapper) {
        this.connection = connection;
        this.mapper = mapper;
    }

    // Subclasses need to define their table name, attributes in db order, and allowed attributes set
    protected abstract List<String> getOrderedAttributes();
    protected abstract Set<String> getAllowedAttributes();

    //Subclasses define based on their specific attributes
    protected abstract void setCreateStatement(PreparedStatement statement, T entity) throws SQLException;
    protected abstract void setUpdateStatement(PreparedStatement statement, T entity) throws SQLException;

    protected String getCreateQuery() {
        String tableName = mapper.getTableName();
        List<String> orderedAttributes = getOrderedAttributes();

        String columns = String.join(", ", orderedAttributes);
        String placeholders = String.join(", ", Collections.nCopies(orderedAttributes.size(), "?"));

        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    protected String getUpdateQuery() {
        String tableName = mapper.getTableName();
        List<String> orderedAttributes = getOrderedAttributes();

        String assignments = orderedAttributes.stream()
                .map(f -> f + " = ?")
                .collect(Collectors.joining(", "));

        return "UPDATE " + tableName + " SET " + assignments + " WHERE id = ?";
    }

    protected String getDeleteQuery() {
        return "DELETE FROM " + mapper.getTableName() + " WHERE id = ?";
    }

    protected String getFindByIdQuery() {
        return "SELECT * FROM " + mapper.getTableName() + " WHERE id = ?";
    }

    protected String getFindAllQuery() {
        return "SELECT * FROM " + mapper.getTableName();
    }

    // -------------------  BASIC CRUD OPERATIONS -----------
    @Override
    public boolean create(T entity) {
        try (PreparedStatement statement = connection.prepareStatement(getCreateQuery())) {
            setCreateStatement(statement, entity);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            // Log exception
            return false;
        }
    }

    @Override
    public boolean update(T entity) {
        try (PreparedStatement statement = connection.prepareStatement(getUpdateQuery())) {
            setUpdateStatement(statement, entity);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            // Log exception
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {

        try (PreparedStatement statement = connection.prepareStatement(getDeleteQuery())) {
            statement.setInt(1, id);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            return false;
        }
    }

    // ----------------- SEARCH QUERIES ------------------
    @Override
    public T findByID(Integer id) {
        return findByAttributes(Map.of("id", String.valueOf(id)))
                .orElse(null);  // preserve original return type
    }

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


    public Optional<T> findByAttributes(Map<String, String> attributes) {
        // Map object attribute names to DB column names
        Map<String, String> dbAttributes = translateAttributeNames(attributes);

        // Build the query
        String query = buildSelectQuery(dbAttributes);

        // Execute the query and map results
        return executeQuery(query, dbAttributes.values().toArray());
    }

    protected String buildSelectQuery(Map<String, String> dbAttributes) {
        String[] conditions = dbAttributes.keySet().stream()
                .map(field -> field + " = ?")
                .toArray(String[]::new);

        return "SELECT * FROM " + mapper.getTableName() + " WHERE " + String.join(" AND ", conditions);
    }

    protected Optional<T> executeQuery(String query, Object[] params) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapper.mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert customer", e);
        }

        return Optional.empty();
    }

    private Map<String, String> translateAttributeNames(Map<String, String> attributes) {
        Map<String, String> dbAttributes = new HashMap<>();
        Map<String, String> attributeMap = mapper.getJavaToDbFieldMap();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = entry.getKey();
            if (!attributeMap.containsKey(attribute)) {
                throw new IllegalArgumentException("Invalid Attribute: " + attribute);
            }
            dbAttributes.put(attributeMap.get(attribute), entry.getValue());
        }

        return dbAttributes;
    }


}
