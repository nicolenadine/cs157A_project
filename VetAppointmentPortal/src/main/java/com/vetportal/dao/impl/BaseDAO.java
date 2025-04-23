package com.vetportal.dao.impl;
import com.vetportal.dao.interfaces.GenericDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public abstract class BaseDAO<T> implements GenericDAO<T> {
    protected Connection connection;

    protected abstract String getCreateQuery();
    protected abstract String getUpdateQuery();
    protected abstract String getDeleteQuery();
    protected abstract String getFindByIdQuery();
    protected abstract String getFindAllQuery();

    protected abstract void setCreateStatement(PreparedStatement statement, T entity) throws SQLException;
    protected abstract void setUpdateStatement(PreparedStatement statement, T entity) throws SQLException;
    protected abstract T extractEntityFromResultSet(ResultSet rs) throws SQLException;

    public BaseDAO(Connection connection) {
        this.connection = connection;
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
        try (PreparedStatement statement = connection.prepareStatement(getFindByIdQuery())) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return extractEntityFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            // Log exception
        }
        return null;
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

    // Ever subclass needs to define how to extract their entity from a ResultSet
    protected abstract T extractEntityFromResultSet(ResultSet rs) throws SQLException;


    public Optional<T> findByFields(Map<String, String> fieldValueMap, Set<String> allowedFields, String tableName) {
        if (fieldValueMap.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided for search.");
        }

        for (String field : fieldValueMap.keySet()) {
            if (!allowedFields.contains(field)) {
                throw new IllegalArgumentException("Invalid field: " + field);
            }
        }

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");
        String[] conditions = fieldValueMap.keySet().stream()
            .map(field -> field + " = ?")
            .toArray(String[]::new);
        queryBuilder.append(String.join(" AND ", conditions));

        try (PreparedStatement statement = connection.prepareStatement(queryBuilder.toString())) {
            int i = 1;
            for (String value : fieldValueMap.values()) {
                statement.setString(i++, value);
            }

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(extractEntityFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find entity with fields: " + fieldValueMap, e);
        }

        return Optional.empty();
    }

}
