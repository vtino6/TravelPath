package com.travelpath.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;
    
    @Column(nullable = false)
    private String url;
    
    private String thumbnailUrl;
    
    @Column(nullable = false)
    private Boolean isCached = false;
    
    private String cachePath;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

