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

    // Subclasses need to define their table name, fields in db order, and allowed fields set
    protected abstract List<String> getOrderedAttributes();
    protected abstract Set<String> getAllowedAttributes();

    //Subclasses define based on their specific attributes
    protected abstract void setCreateStatement(PreparedStatement statement, T entity) throws SQLException;
    protected abstract void setUpdateStatement(PreparedStatement statement, T entity) throws SQLException;

    public BaseDAO(Connection connection, EntityMapper<T> mapper) {
        this.connection = connection;
        this.mapper = mapper;
    }

    protected String getCreateQuery() {
        String tableName = mapper.getTableName();
        List<String> fields = getOrderedAttributes();

        String columns = String.join(", ", fields);
        String placeholders = String.join(", ", Collections.nCopies(fields.size(), "?"));

        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    protected String getUpdateQuery() {
        String tableName = mapper.getTableName();
        List<String> fields = getOrderedAttributes();

        String assignments = fields.stream()
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
            // Log exception
            return false;
        }
    }

    // ----------------- SEARCH QUERIES ------------------
    @Override
    public T findByID(Integer id) {
        return findByFields(Map.of("id", String.valueOf(id)))
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
            // Log or rethrow exception
            throw new DataAccessException("Error fetching all records from " + mapper.getTableName(), e);
        }
        return entities;
    }


    public Optional<T> findByFields(Map<String, String> javaFields) {
        // 1. Map Java field names to DB column names
        Map<String, String> dbFields = translateFieldNames(javaFields);

        // 2. Build the query
        String query = buildSelectQuery(dbFields);

        // 3. Execute the query and map results
        return executeQuery(query, dbFields.values().toArray());
    }

    // LOGGING VERSION REMOVE LATER
    protected String buildSelectQuery(Map<String, String> dbFields) {
        String[] conditions = dbFields.keySet().stream()
                .map(field -> field + " = ?")
                .toArray(String[]::new);

        String sql = "SELECT * FROM " + mapper.getTableName() + " WHERE " + String.join(" AND ", conditions);

        System.out.println("Generated SQL (BaseDAO): " + sql);

        return sql;
    }


//    protected String buildSelectQuery(Map<String, String> dbFields) {
//        String[] conditions = dbFields.keySet().stream()
//                .map(field -> field + " = ?")
//                .toArray(String[]::new);
//
//        return "SELECT * FROM " + mapper.getTableName() + " WHERE " + String.join(" AND ", conditions);
//    }

    protected Optional<T> executeQuery(String query, Object[] params) {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapper.mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query error", e);
        }

        return Optional.empty();
    }

    private Map<String, String> translateFieldNames(Map<String, String> javaFields) {
        Map<String, String> dbFields = new HashMap<>();
        Map<String, String> fieldMap = mapper.getJavaToDbFieldMap();

        for (Map.Entry<String, String> entry : javaFields.entrySet()) {
            String javaField = entry.getKey();
            if (!fieldMap.containsKey(javaField)) {
                throw new IllegalArgumentException("Invalid field: " + javaField);
            }
            dbFields.put(fieldMap.get(javaField), entry.getValue());
        }

        return dbFields;
    }


}
