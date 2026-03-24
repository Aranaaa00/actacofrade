package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByCode(RoleCode code);
}
