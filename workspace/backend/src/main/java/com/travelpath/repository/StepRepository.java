package com.travelpath.repository;

import com.travelpath.model.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepRepository extends JpaRepository<Step, String> {
    
    List<Step> findByRouteIdOrderByOrderAsc(String routeId);
    
    @Query("SELECT s FROM Step s WHERE s.route.id = :routeId ORDER BY s.order ASC")
    List<Step> findStepsByRouteId(@Param("routeId") String routeId);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM steps WHERE route_id = :routeId", nativeQuery = true)
    void deleteByRouteId(@Param("routeId") String routeId);
}

