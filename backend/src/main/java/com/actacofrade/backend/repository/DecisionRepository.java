package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Integer>, JpaSpecificationExecutor<Decision> {

    List<Decision> findByEventId(Integer eventId);
}
