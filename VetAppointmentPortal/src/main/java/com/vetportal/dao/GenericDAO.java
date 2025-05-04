package com.vetportal.dao;

import java.util.List;
import java.util.Optional;

public interface GenericDAO<T> {
    boolean create(T entity);
    boolean update(T entity);
    boolean delete(Integer id);
    Optional<T> findByID(Integer id);
    List<T> findAll();
}