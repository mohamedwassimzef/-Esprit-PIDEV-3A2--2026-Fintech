package tn.esprit.dao;

import java.util.List;

/**
 * Generic CRUD interface for Data Access Object (DAO) implementations.
 * This interface provides basic CRUD (Create, Read, Update, Delete) operations
 * for any entity type.
 *
 * @param <T> The entity type
 */
public interface CrudInterface<T> {

    /**
     * Create a new entity in the database.
     *
     * @param entity The entity to create
     * @return true if creation was successful, false otherwise
     */
    boolean create(T entity);

    /**
     * Read an entity from the database by its ID.
     *
     * @param id The ID of the entity to retrieve
     * @return The entity if found, null otherwise
     */
    T read(int id);

    /**
     * Retrieve all entities of this type from the database.
     *
     * @return A list of all entities
     */
    List<T> readAll();

    /**
     * Update an existing entity in the database.
     *
     * @param entity The entity to update
     * @return true if update was successful, false otherwise
     */
    boolean update(T entity);

    /**
     * Delete an entity from the database by its ID.
     *
     * @param id The ID of the entity to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean delete(int id);
}

