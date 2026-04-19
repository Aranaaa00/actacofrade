package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {

    Optional<Event> findByReference(String reference);

    Optional<Event> findByIdAndHermandadId(Integer id, Integer hermandadId);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(reference FROM 6) AS INTEGER)) FROM events WHERE reference LIKE :prefix",
            nativeQuery = true)
    Integer findMaxReferenceNumberByYearPrefix(@Param("prefix") String prefix);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status NOT IN ('COMPLETED', 'REJECTED')",
            nativeQuery = true)
    long countPendingTasksByEventId(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status = 'PLANNED'",
            nativeQuery = true)
    long countTasksWithPlannedStatus(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status = 'REJECTED'",
            nativeQuery = true)
    long countTasksWithRejectedStatus(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status = 'IN_PREPARATION'",
            nativeQuery = true)
    long countTasksWithInPreparationStatus(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status = 'ACCEPTED'",
            nativeQuery = true)
    long countTasksWithAcceptedStatus(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status = 'CONFIRMED'",
            nativeQuery = true)
    long countTasksWithConfirmedStatus(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId AND status = 'COMPLETED'",
            nativeQuery = true)
    long countTasksWithCompletedStatus(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE event_id = :eventId",
            nativeQuery = true)
    long countTotalTasksByEventId(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM incidents WHERE event_id = :eventId AND status = 'ABIERTA'",
            nativeQuery = true)
    long countOpenIncidentsByEventId(@Param("eventId") Integer eventId);

    @Query(value = "SELECT COUNT(*) FROM decisions WHERE event_id = :eventId AND status = 'PENDING'",
            nativeQuery = true)
    long countPendingDecisionsByEventId(@Param("eventId") Integer eventId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO event_clones (original_event_id, cloned_event_id) VALUES (:originalId, :clonedId)",
            nativeQuery = true)
    void insertCloneRelation(@Param("originalId") Integer originalId, @Param("clonedId") Integer clonedId);
}
