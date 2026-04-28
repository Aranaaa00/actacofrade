package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Incident;
import com.actacofrade.backend.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Integer>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByEventId(Integer eventId);

    List<Incident> findByReportedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(
            Integer userId, IncidentStatus status, Integer hermandadId);
}
