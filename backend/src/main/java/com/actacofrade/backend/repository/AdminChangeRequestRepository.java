package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.AdminChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data access for admin change requests. */
public interface AdminChangeRequestRepository extends JpaRepository<AdminChangeRequest, Integer> {

    /** Returns every request ordered by creation date (newest first). */
    List<AdminChangeRequest> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("DELETE FROM AdminChangeRequest a WHERE a.hermandad.id = :hermandadId")
    int deleteByHermandadId(@Param("hermandadId") Integer hermandadId);
}
