package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
	
    @Autowired
    private ItemRepository itemRepository;
    
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    
    // Thread-safe list to store processed items
    private List<Item> processedItems = Collections.synchronizedList(new ArrayList<>()); 
    
 // Thread-safe counter to track the number of processed items
    private AtomicInteger processedCount = new AtomicInteger(0);//used AtomicInteger instead of int -> thread-safe


    // CRUD methods
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
    	
    	// Fetch IDs of all items from the repository
        List<Long> itemIds = itemRepository.findAllIds();
        
        // Store submitted task futures for tracking and waiting
        List<Future<?>> futures = new ArrayList<>();
        
        // Process each item asynchronously using executor threads
        for (Long id : itemIds) {
            Future<?> future = executor.submit(() -> {
                try {
                    Thread.sleep(100);

                    // Attempt to fetch item by ID
                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isEmpty()) {
                        return; // Skip if not found
                    }
                    
                    // Update item status and save
                    Item item = optionalItem.get();
                    
                    item.setStatus("PROCESSED");
                    
                    itemRepository.save(item);
                    
                    processedItems.add(item);
                    
                    processedCount.incrementAndGet(); // modified incremental method to suit the AtomicInteger type

                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            });
            
            futures.add(future);
        }
        
        // Wait for all submitted tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get(); // blocks until task is done
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error in task: " + e.getMessage());
            }
        }
        // Return the result wrapped in a completed CompletableFuture
        return CompletableFuture.completedFuture(processedItems);
    }
    
}

