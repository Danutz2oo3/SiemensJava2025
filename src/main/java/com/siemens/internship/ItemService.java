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
    
    private List<Item> processedItems = Collections.synchronizedList(new ArrayList<>()); //replaced normal list with a thread-safe version
    
    private AtomicInteger processedCount = new AtomicInteger(0);//used AtomicInteger instead of int -> thread-safe


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
    public List<Item> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();
        
        List<Future<?>> futures = new ArrayList<>();
        
        for (Long id : itemIds) {
            Future<?> future = executor.submit(() -> {
                try {
                    Thread.sleep(100);

                    //Item item = itemRepository.findById(id).orElse(null);
                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isEmpty()) {
                        return;
                    }

                    Item item = optionalItem.get();
                    
                    item.setStatus("PROCESSED");
                    
                    itemRepository.save(item);
                    
                    processedItems.add(item);
                    
                    processedCount.incrementAndGet();

                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            });
            
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get(); // blocks until task is done
            } catch (InterruptedException | ExecutionException e) {
                // Log the failure of that specific task
                System.out.println("Error in task: " + e.getMessage());
            }
        }

        return processedItems;
    }

}

