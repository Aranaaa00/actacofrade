package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {

    Optional<Event> findByReference(String reference);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(reference FROM 6) AS INTEGER)) FROM events WHERE reference LIKE :prefix",
            nativeQuery = true)
    Integer findMaxReferenceNumberByYearPrefix(@Param("prefix") String prefix);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status != 'CONFIRMADA'",
            nativeQuery = true)
    long countPendingTasksByEventId(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM incidents WHERE event_id = :eventId AND status = 'ABIERTA'",
            nativeQuery = true)
    long countOpenIncidentsByEventId(@Param("eventId") Integer eventId);
}
