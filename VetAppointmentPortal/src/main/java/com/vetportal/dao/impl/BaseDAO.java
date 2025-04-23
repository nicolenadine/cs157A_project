package com.vetportal.dao.impl;
import com.vetportal.dao.interfaces.GenericDAO;
import com.vetportal.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseDAO<T> implements GenericDAO<T> {
    protected Connection connection;

    // Subclasses need to define their table name, fields in db order, and allowed fields set
    protected abstract String getTableName();
    protected abstract List<String> getOrderedAttributes();
    protected abstract Set<String> getAllowedAttributes();

    //Subclasses define based on their specific attributes
    protected abstract void setCreateStatement(PreparedStatement statement, T entity) throws SQLException;
    protected abstract void setUpdateStatement(PreparedStatement statement, T entity) throws SQLException;
    protected abstract T extractEntityFromResultSet(ResultSet rs) throws SQLException;

    public BaseDAO(Connection connection) {
        this.connection = connection;
    }

    protected String getCreateQuery() {
        String table = getTableName();
        List<String> fields = getOrderedAttributes();

        String columns = String.join(", ", fields);
        String placeholders = String.join(", ", Collections.nCopies(fields.size(), "?"));

        return "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    protected String getUpdateQuery() {
        String table = getTableName();
        List<String> fields = getOrderedAttributes();

        String assignments = fields.stream()
                .map(f -> f + " = ?")
                .collect(Collectors.joining(", "));

        return "UPDATE " + table + " SET " + assignments + " WHERE id = ?";
    }

    protected String getDeleteQuery() {
        return "DELETE FROM " + getTableName() + " WHERE id = ?";
    }

    protected String getFindByIdQuery() {
        return "SELECT * FROM " + getTableName() + " WHERE id = ?";
    }

    protected String getFindAllQuery() {
        return "SELECT * FROM " + getTableName();
    }

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
                entities.add(extractEntityFromResultSet(rs));
            }
        } catch (SQLException e) {
            // Log exception
        }
        return entities;
    }


    public Optional<T> findByFields(Map<String, String> fields) {
        Set<String> allowedFields = getAllowedAttributes();
        String tableName = getTableName();

        if (fields.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be specified");
        }

        for (String key : fields.keySet()) {
            if (!allowedFields.contains(key)) {
                throw new IllegalArgumentException("Invalid field: " + key);
            }
        }

        String query = buildSelectQuery(fields, tableName);

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            int i = 1;
            for (String value : fields.values()) {
                stmt.setString(i++, value);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(extractEntityFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query error for: " + fields, e);
        }

        return Optional.empty();
    }

    private String buildSelectQuery(Map<String, String> fields, String tableName) {
        String[] conditions = fields.keySet().stream()
                .map(field -> field + " = ?")
                .toArray(String[]::new);
        return "SELECT * FROM " + tableName + " WHERE " + String.join(" AND ", conditions);
    }


}
