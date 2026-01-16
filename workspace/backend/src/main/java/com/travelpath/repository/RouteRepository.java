package com.travelpath.repository;

import com.travelpath.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {
    
    List<Route> findByUserId(String userId);
    
    List<Route> findByUserIdAndIsSavedTrue(String userId);
    
    List<Route> findByUserIdAndIsFavoriteTrue(String userId);
    
    List<Route> findByUserIdIsNull();
    
    // Query by userIdString (works even if User entity doesn't exist)
    @Query("SELECT r FROM Route r WHERE (r.userIdString = :userId OR (r.user IS NOT NULL AND r.user.id = :userId)) AND r.isSaved = true ORDER BY r.createdAt DESC")
    List<Route> findSavedRoutesByUser(@Param("userId") String userId);
    
    // Query routes by userIdString only (for routes where User entity doesn't exist)
    @Query("SELECT r FROM Route r WHERE r.userIdString = :userId AND r.isSaved = true ORDER BY r.createdAt DESC")
    List<Route> findSavedRoutesByUserIdString(@Param("userId") String userId);
    
    // Query routes with null userIdString (anonymous routes)
    @Query("SELECT r FROM Route r WHERE r.userIdString IS NULL AND r.isSaved = true ORDER BY r.createdAt DESC")
    List<Route> findSavedRoutesByUserIdStringIsNull();
    
    Optional<Route> findByIdAndUserId(String id, String userId);
}

