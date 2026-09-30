package com.iwfc.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface Repository<T extends Identifiable<ID>, ID> {

    T add(T item);

    T update(T item);

    Optional<T> findById(ID id);

    T getById(ID id);

    List<T> findAll();

    List<T> findBy(Predicate<T> condition);

    boolean existsById(ID id);

    boolean delete(ID id);

    int count();
}
