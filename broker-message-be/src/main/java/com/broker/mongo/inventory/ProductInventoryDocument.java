package com.broker.mongo.inventory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "productos")
public class ProductInventoryDocument {

    @Id
    private String id;

    private String name;

    private String image;

    private Integer availableQuantity = 0;

    private Set<String> appliedInventoryEvents = new HashSet<>();

    private LocalDateTime updatedAt = LocalDateTime.now();
}