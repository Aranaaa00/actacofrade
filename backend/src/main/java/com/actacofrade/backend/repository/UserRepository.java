package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByHermandadId(Integer hermandadId);

    long countByHermandadIdAndActiveTrue(Integer hermandadId);

    Optional<User> findByIdAndHermandadId(Integer id, Integer hermandadId);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
           "WHERE u.hermandad.id = :hermandadId " +
           "AND u.active = true " +
           "AND u.id NOT IN (" +
           "  SELECT u2.id FROM User u2 JOIN u2.roles r2 WHERE r2.code = :excludedRole" +
           ")")
    List<User> findAssignableByHermandadId(@Param("hermandadId") Integer hermandadId,
                                           @Param("excludedRole") RoleCode excludedRole);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
           "WHERE u.hermandad.id = :hermandadId AND r.code = :roleCode")
    long countByHermandadIdAndRoleCode(@Param("hermandadId") Integer hermandadId,
                                       @Param("roleCode") RoleCode roleCode);

    long countByHermandadId(Integer hermandadId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM User u WHERE u.hermandad.id = :hermandadId")
    int deleteByHermandadId(@Param("hermandadId") Integer hermandadId);
}
