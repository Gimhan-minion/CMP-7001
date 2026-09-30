package com.iwfc.repository;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.EntityNotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemoryRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {

    private final Map<ID, T> store = new LinkedHashMap<>();
    private final String entityName;

    public InMemoryRepository(String entityName) {
        this.entityName = entityName;
    }

    @Override
    public T add(T item) {
        if (store.containsKey(item.getId())) {
            throw new DuplicateDataException(entityName + " with id " + item.getId() + " already exists");
        }
        store.put(item.getId(), item);
        return item;
    }

    @Override
    public T update(T item) {
        if (!store.containsKey(item.getId())) {
            throw new EntityNotFoundException(entityName + " " + item.getId() + " not found");
        }
        store.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public T getById(ID id) {
        return findById(id).orElseThrow(
                () -> new EntityNotFoundException(entityName + " " + id + " not found"));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<T> findBy(Predicate<T> condition) {
        return store.values().stream().filter(condition).collect(Collectors.toList());
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public boolean delete(ID id) {
        return store.remove(id) != null;
    }

    @Override
    public int count() {
        return store.size();
    }
}
