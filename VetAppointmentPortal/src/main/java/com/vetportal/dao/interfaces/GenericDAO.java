package com.vetportal.dao.interfaces;

import java.util.List;

public interface GenericDAO<T> {
    boolean create(T entity);
    boolean update(T entity);
    boolean delete(Integer id);
    T findByID(Integer id);
    List<T> findAll();
}