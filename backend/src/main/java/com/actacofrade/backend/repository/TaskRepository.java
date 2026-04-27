package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer>, JpaSpecificationExecutor<Task> {

    List<Task> findByEventId(Integer eventId);

    long countByAssignedToIdAndStatusAndEventHermandadId(Integer userId, TaskStatus status, Integer hermandadId);

    long countByAssignedToIdAndStatusInAndEventHermandadId(Integer userId, Collection<TaskStatus> statuses, Integer hermandadId);
}
