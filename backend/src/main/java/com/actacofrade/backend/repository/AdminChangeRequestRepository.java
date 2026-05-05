package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.AdminChangeRequest;
import com.actacofrade.backend.entity.AdminChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminChangeRequestRepository extends JpaRepository<AdminChangeRequest, Integer> {

    List<AdminChangeRequest> findAllByOrderByCreatedAtDesc();

    List<AdminChangeRequest> findByStatusOrderByCreatedAtDesc(AdminChangeRequestStatus status);

    boolean existsByHermandadIdAndStatus(Integer hermandadId, AdminChangeRequestStatus status);
}
