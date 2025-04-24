package com.vetportal.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface EntityMapper<T> {
    Map<String, String> getJavaToDbFieldMap();
    String getTableName();
    T mapResultSetToEntity(ResultSet rs) throws SQLException;
}