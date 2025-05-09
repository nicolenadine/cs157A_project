package com.vetportal.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

// Defines the essential functions for mapping between Java Entities and Database Entities
public interface EntityMapper<T> {
    Map<String, String> getJavaToDbAttributeMap();
    String getTableName();
    T mapResultSetToEntity(ResultSet rs) throws SQLException;
}