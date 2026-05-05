package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.AdminChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for admin change requests. */
public interface AdminChangeRequestRepository extends JpaRepository<AdminChangeRequest, Integer> {

    /** Returns every request ordered by creation date (newest first). */
    List<AdminChangeRequest> findAllByOrderByCreatedAtDesc();
}
