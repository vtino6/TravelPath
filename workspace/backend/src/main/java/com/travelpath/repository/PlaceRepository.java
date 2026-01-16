package com.travelpath.repository;

import com.travelpath.model.Place;
import com.travelpath.model.PlaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {
    
    List<Place> findByCategory(PlaceCategory category);
    
    @Query(value = "SELECT * FROM places WHERE " +
           "6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
           "cos(radians(longitude) - radians(:lng)) + " +
           "sin(radians(:lat)) * sin(radians(latitude))) <= :radius",
           nativeQuery = true)
    List<Place> findNearby(
        @Param("lat") double latitude,
        @Param("lng") double longitude,
        @Param("radius") double radiusKm
    );
    
    @Query(value = "SELECT * FROM places WHERE category = :category AND " +
           "6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
           "cos(radians(longitude) - radians(:lng)) + " +
           "sin(radians(:lat)) * sin(radians(latitude))) <= :radius",
           nativeQuery = true)
    List<Place> findNearbyByCategory(
        @Param("lat") double latitude,
        @Param("lng") double longitude,
        @Param("radius") double radiusKm,
        @Param("category") String category
    );
}

