package com.siemens.internship;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @NotBlank(message = "Name is required") // Validation added
    private String name;
    
    private String description;
    
    @NotBlank(message = "Status is required") // Validation added
    private String status;

    @Email(message = "Invalid email format") // Validation added
    @NotBlank(message = "Email is required")
    private String email;
    
    /**
     * Represents a persistable item with name, status, and contact email.
     * Used in the CRUD operations and processed asynchronously in batch mode.
     */
}