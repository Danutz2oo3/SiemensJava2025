package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * GET /api/items
     * Retrieves all items from the database.
     *
     * @return 200 OK with a list of items
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    /**
    * POST /api/items
    * Creates a new item if the request body is valid.
    *
    * @param item The item to create (validated)
    * @param result BindingResult to check for validation errors
    * @return 201 CREATED on success, 400 BAD REQUEST if validation fails
    */
    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
        	return new ResponseEntity<>(HttpStatus.BAD_REQUEST);//modified request status to BAD_REQUEST
        }
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);//modified status to CREATED
    }

    /**
     * GET /api/items/{id}
     * Retrieves a specific item by ID.
     *
     * @param id The ID of the item
     * @return 200 OK if found, 404 NOT FOUND if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));//modified status to NOT_FOUND
    }
    
    /**
     * PUT /api/items/{id}
     * Updates an existing item by ID if it exists.
     *
     * @param id   The ID of the item to update
     * @param item The updated item content
     * @return 200 OK if updated, 404 NOT FOUND if item doesn't exist
     */
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /api/items/{id}
     * Deletes an item by ID if it exists.
     *
     * @param id The ID of the item to delete
     * @return 204 NO CONTENT if deleted, 404 NOT FOUND if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
    	Optional<Item> existing = itemService.findById(id);
        if (existing.isPresent()) {
            itemService.deleteById(id);

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /api/items/process
     * Triggers asynchronous processing of all items.
     * Waits for all tasks to complete and returns the list of processed items.
     *
     * @return 200 OK with processed items list, 500 INTERNAL SERVER ERROR if async task fails
     */
    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        try {
            List<Item> result = itemService.processItemsAsync().get();
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (InterruptedException | ExecutionException e) {
            // Return a generic 500 response
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
}
