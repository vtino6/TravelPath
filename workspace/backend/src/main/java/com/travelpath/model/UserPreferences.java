package com.travelpath.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
    
    private Double maxBudget;
    
    private Integer duration; // in minutes
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EffortLevel effortLevel;
    
    @Column(nullable = false)
    private Integer coldSensitivity = 0;
    
    @Column(nullable = false)
    private Integer heatSensitivity = 0;
    
    @Column(nullable = false)
    private Integer humiditySensitivity = 0;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

