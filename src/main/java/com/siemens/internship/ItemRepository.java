package com.siemens.internship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Added the @Repository annotation
public interface ItemRepository extends JpaRepository<Item, Long> {
	
	/**
     * Retrieves all item IDs from the database.
     * @return List of item IDs
     */
    @Query("SELECT id FROM Item")
    List<Long> findAllIds();
}
